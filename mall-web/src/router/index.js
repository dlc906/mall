import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue')
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('../views/ProductDetail.vue')
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('../views/Cart.vue')
      },
      {
        path: 'order/confirm',
        name: 'OrderConfirm',
        component: () => import('../views/OrderConfirm.vue')
      },
      {
        path: 'order/list',
        name: 'OrderList',
        component: () => import('../views/OrderList.vue')
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('../views/OrderDetail.vue')
      },
      {
        path: 'user/profile',
        name: 'UserProfile',
        component: () => import('../views/UserProfile.vue')
      },
      {
        path: 'user/address',
        name: 'AddressList',
        component: () => import('../views/AddressList.vue')
      },
      {
        path: 'distribution',
        name: 'Distribution',
        component: () => import('../views/Distribution.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Auth guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  if (!to.meta.noAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
