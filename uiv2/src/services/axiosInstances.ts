///
/// Licensed to The OpenNMS Group, Inc (TOG) under one or more
/// contributor license agreements.  See the LICENSE.md file
/// distributed with this work for additional information
/// regarding copyright ownership.
///
/// TOG licenses this file to You under the GNU Affero General
/// Public License Version 3 (the "License") or (at your option)
/// any later version.  You may not use this file except in
/// compliance with the License.  You may obtain a copy of the
/// License at:
///
///      https://www.gnu.org/licenses/agpl-3.0.txt
///
/// Unless required by applicable law or agreed to in writing,
/// software distributed under the License is distributed on an
/// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
/// either express or implied.  See the License for the specific
/// language governing permissions and limitations under the
/// License.
///

import axios, {AxiosRequestConfig} from 'axios'
import {useAuthStore} from "@/stores/authStore.ts";
import {router} from "@/router";
import {Router} from "vue-router";

const v2 = axios.create({
    baseURL: import.meta.env.VITE_BASE_V2_URL?.toString() || '/opennms/api/v2',
})

const rest = axios.create({
    baseURL: import.meta.env.VITE_BASE_REST_URL?.toString() || '/opennms/rest',
})

const restFile = axios.create({
    baseURL: import.meta.env.VITE_BASE_REST_URL?.toString() || '/opennms/rest',
    headers: {
        'Content-Type': 'multipart/form-data'
    }
})

// Ensure all instances get proper interception
const instances = [v2, rest, restFile];
instances.forEach(it => {
    // Ensure basic auth is sent with each request
    it.interceptors.request.use((config) => {
        if (router.currentRoute.value.path != '/login' && config.baseURL?.startsWith(import.meta.env.VITE_BASE_URL)) {
            const authStore = useAuthStore()
            const token = authStore.login;
            if (token) {
                config.headers.set('Authorization', `Basic ${token}`);
            } else {
                config.headers.delete('Authorization');
            }
        }
        return config;
    });

    // Responses of 401 and 403 are redirected if not login page to the login page
    it.interceptors.response.use((response) => {
        return response;
    }, (error) => {
        const authStore = useAuthStore()
        // If not on the login page, redirect to login page on 401
        if (router.currentRoute.value.path != '/login' && error.response.status === 401) {
            authStore.logout();
            console.log("NOOOO");
            return router.push('/login')
        }
        // If not login page, and 403 redirect to forbidden
        if (router.currentRoute.value.path != '/login' && error.response.status === 403) {
            return router.push('/forbidden');
        }
        return Promise.reject(error)
    })
})


export {v2, rest, restFile}
