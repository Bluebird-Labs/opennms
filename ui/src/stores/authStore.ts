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

import {defineStore} from 'pinia'
import API from '@/services'
import {WhoAmIResponse} from '@/types'

export type Credentials = {
  username: string
  password: string
}

// TODO MVR rework this probably?
export const useAuthStore = defineStore('authStore', () => {
  const whoAmI = ref({roles: [] as string[]} as WhoAmIResponse)
  const loaded = ref(false)
  const login = ref()
  const returnUrl = ref();

  const getWhoAmI = async () => {
    whoAmI.value = await API.getWhoAmI()
  }

  const setLogin = (credentials: Credentials) => {
    login.value = btoa(`${credentials.username}:${credentials.password}`)
  }

  const isLogin = () => {
    return login.value != null;
  }

  const logout = () => {
    login.value = null;
  }

  return {
    returnUrl,
    loaded,
    whoAmI,
    login,
    getWhoAmI,
    setLogin,
    isLogin,
    logout
  }
})
