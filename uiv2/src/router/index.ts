import {createRouter, createWebHistory} from 'vue-router';

import {useAuthStore} from '@/stores/authStore.ts';
import LoginView from '@/views/LoginView.vue';
import HomeView from '@/views/HomeView.vue';
import PasswordGateView from "@/views/PasswordGateView.vue";

export const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    linkActiveClass: 'active',
    routes: [
        {path: '/', component: HomeView},
        {path: '/login', component: LoginView},
        {path: '/passwordGate', component: PasswordGateView}
    ]
});

// router.beforeEach(() => startSpinner())
// router.afterEach(() => stopSpinner())

router.beforeEach(async (to) => {
    // redirect to login page if not logged in and trying to access a restricted page
    // const publicPages = ['/login'];
    // const authRequired = !publicPages.includes(to.path);
    // const auth = useAuthStore();
    // if (authRequired && !auth.isLogin()) {
    //     auth.returnUrl = to.fullPath;
    //     return '/login';
    // }
});