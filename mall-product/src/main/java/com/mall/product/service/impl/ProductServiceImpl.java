package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.constant.RedisKey;
import com.mall.common.exception.BizException;
import com.mall.common.utils.RedisUtils;
import com.mall.product.entity.Product;
import com.mall.product.mapper.ProductMapper;
import com.mall.product.model.es.ProductDocument;
import com.mall.product.model.req.ProductReq;
import com.mall.product.repository.ProductEsRepository;
import com.mall.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    /** 空值标记（防穿透）：商品不存在时缓存此标记 */
    private static final String EMPTY_MARK = "EMPTY_PRODUCT";
    /** 空值缓存 TTL：60 秒 */
    private static final long EMPTY_TTL_SECONDS = 60;
    /** 缓存基础 TTL：5 分钟 */
    private static final long BASE_TTL_SECONDS = 5 * 60;
    /** TTL 随机偏移上限：0~120 秒（防雪崩） */
    private static final long TTL_JITTER_SECONDS = 120;
    /** 防击穿互斥锁 TTL：3 秒 */
    private static final long LOCK_TIMEOUT_SECONDS = 3;

    @Resource
    private ProductMapper productMapper;
    @Autowired(required = false)
    private ProductEsRepository productEsRepository;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public Page<Product> pageProducts(int pageNum, int pageSize, Long categoryId, String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSort)
                .orderByDesc(Product::getCreateTime);

        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword)
                   .or().like(Product::getDescription, keyword);
        }

        return productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Product getProductDetail(Long id) {
        // ========== 缓存三防：防穿透 + 防击穿 + 防雪崩 ==========
        String cacheKey = RedisKey.PRODUCT_INFO + id;

        // 1. 读缓存
        Object cached = redisUtils.get(cacheKey);
        if (cached != null) {
            // 命中空值标记（防穿透缓存）→ 直接返回不存在
            if (EMPTY_MARK.equals(cached)) {
                throw new BizException("商品不存在");
            }
            return (Product) cached;
        }

        // 2. 缓存未命中 → 互斥锁防击穿（只有一个线程查库回填，其余等待后重读缓存）
        String lockKey = RedisKey.LOCK_PREFIX + "product:info:" + id;
        boolean locked = redisUtils.setIfAbsent(lockKey, "1", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (locked) {
            try {
                Product product = productMapper.selectById(id);
                if (product == null) {
                    // 防穿透：缓存空值标记（短 TTL），避免恶意请求反复打 DB
                    redisUtils.set(cacheKey, EMPTY_MARK, EMPTY_TTL_SECONDS, TimeUnit.SECONDS);
                    throw new BizException("商品不存在");
                }
                // 防雪崩：随机 TTL（5分钟基础 + 0~120秒随机偏移），避免大量缓存同时失效
                long ttl = BASE_TTL_SECONDS
                        + ThreadLocalRandom.current().nextLong(0, TTL_JITTER_SECONDS + 1);
                redisUtils.set(cacheKey, product, ttl, TimeUnit.SECONDS);
                return product;
            } finally {
                redisUtils.delete(lockKey);
            }
        }

        // 3. 未拿到锁：等其他线程回填完成后重读缓存
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Object retry = redisUtils.get(cacheKey);
        if (retry != null) {
            if (EMPTY_MARK.equals(retry)) {
                throw new BizException("商品不存在");
            }
            return (Product) retry;
        }

        // 4. 兜底：锁已释放/超时且缓存仍未回填，直接查库（避免无限等待）
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        long ttl = BASE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(0, TTL_JITTER_SECONDS + 1);
        redisUtils.set(cacheKey, product, ttl, TimeUnit.SECONDS);
        return product;
    }

    @Override
    @Transactional
    public Product addProduct(ProductReq req) {
        Product product = new Product();
        BeanUtils.copyProperties(req, product);
        if (product.getStatus() == null) product.setStatus(1);
        if (product.getSales() == null) product.setSales(0);
        productMapper.insert(product);

        // 清除可能存在的空值缓存（防穿透标记），避免新商品 60 秒内查不到
        redisUtils.delete(RedisKey.PRODUCT_INFO + product.getId());

        // Sync to ES
        syncToEs(product);

        // Cache stock
        String stockKey = RedisKey.PRODUCT_STOCK + product.getId();
        redisUtils.set(stockKey, product.getStock());

        return product;
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, ProductReq req) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BizException("商品不存在");

        BeanUtils.copyProperties(req, product, "id");
        productMapper.updateById(product);

        // Clear Redis cache
        redisUtils.delete(RedisKey.PRODUCT_INFO + id);

        // Update ES
        syncToEs(product);

        // Update Redis stock if changed
        if (req.getStock() != null) {
            redisUtils.set(RedisKey.PRODUCT_STOCK + id, req.getStock());
        }

        return product;
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
        redisUtils.delete(RedisKey.PRODUCT_INFO + id);
        redisUtils.delete(RedisKey.PRODUCT_STOCK + id);
        if (productEsRepository != null) {
            productEsRepository.deleteById(id);
        }
    }

    @Override
    public void updateStock(Long id, int quantity) {
        String stockKey = RedisKey.PRODUCT_STOCK + id;

        // Redis 库存 key 缺失时（服务重启/Redis 重启/缓存丢失），从 MySQL 加载初始库存
        Object current = redisUtils.get(stockKey);
        if (current == null) {
            Product p = productMapper.selectById(id);
            if (p == null) {
                throw new BizException("商品不存在");
            }
            redisUtils.set(stockKey, p.getStock());
            log.info("Stock key initialized from MySQL: productId={}, stock={}", id, p.getStock());
        }

        // Atomically decrement stock in Redis
        long newStock = redisUtils.increment(stockKey, quantity); // quantity is negative

        if (newStock < 0) {
            redisUtils.increment(stockKey, -quantity); // rollback
            throw new BizException("库存不足");
        }

        // Async update MySQL (in production, use MQ)
        Product product = new Product();
        product.setId(id);
        product.setStock((int) newStock);
        productMapper.updateById(product);

        // 清除商品详情缓存，下次查询时从 MySQL 加载最新数据
        redisUtils.delete(RedisKey.PRODUCT_INFO + id);
    }

    @Override
    public void incrementSales(Long id, int quantity) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            log.warn("Product {} not found, cannot increment sales", id);
            return;
        }
        product.setSales(product.getSales() == null ? quantity : product.getSales() + quantity);
        productMapper.updateById(product);
        // 清除商品详情缓存
        redisUtils.delete(RedisKey.PRODUCT_INFO + id);
        log.info("Product {} sales incremented by {}, now: {}", id, quantity, product.getSales());
    }

    @Override
    public Page<ProductDocument> searchFromEs(String keyword, Long categoryId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);

        org.springframework.data.domain.Page<ProductDocument> result;
        if (categoryId != null && categoryId > 0) {
            result = productEsRepository.findByCategoryId(categoryId, pageable);
        } else if (StringUtils.hasText(keyword)) {
            result = productEsRepository.findByNameOrDescriptionOrKeywords(keyword, keyword, keyword, pageable);
        } else {
            result = productEsRepository.findAll(pageable);
        }

        // Wrap in MyBatis-Plus Page for consistency
        Page<ProductDocument> page = new Page<>(pageNum, pageSize);
        page.setTotal(result.getTotalElements());
        page.setRecords(result.getContent());
        return page;
    }

    @Override
    public void syncAllToEs() {
        if (productEsRepository == null) {
            log.warn("Elasticsearch not available, skip sync");
            return;
        }
        List<Product> products = productMapper.selectList(null);
        List<ProductDocument> docs = products.stream().map(p -> {
            ProductDocument doc = new ProductDocument();
            BeanUtils.copyProperties(p, doc);
            return doc;
        }).collect(Collectors.toList());
        productEsRepository.saveAll(docs);
        log.info("Synced {} products to Elasticsearch", products.size());
    }

    private void syncToEs(Product product) {
        if (productEsRepository == null) return;
        ProductDocument doc = new ProductDocument();
        BeanUtils.copyProperties(product, doc);
        productEsRepository.save(doc);
    }
}
