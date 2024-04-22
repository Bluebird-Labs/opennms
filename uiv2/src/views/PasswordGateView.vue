<script setup lang="ts">


import LoginLogoHorizon from "@/assets/LoginLogoHorizon.vue";
import Logo from "@/assets/Logo.vue";
import {router} from "@/router";
import {useAuthStore} from "@/stores/authStore.ts";

const changePassword = () => {
  console.log("CLICKED");
}

const skip = () => {
  const store = useAuthStore();
  router.push(store.returnUrl || '/');
}

</script>

<template>
  <div class="login-page">
    <div class="card login-form rounded">
      <div style="padding-bottom: 36px; padding-top: 60px">
        <LoginLogoHorizon/>
      </div>
      <div class="form-content-wrapper">
        <form role="form">
          <div class="form-content">
            <div class="form-group">
                        <span class="pg-text">
                            Please take a moment to change your <em>admin</em> user password from its default value.
                            This step helps protect your installation against
                            <a class="pg-link" target="_blank" rel="noopener"
                               href="https://www.cisa.gov/news-events/cybersecurity-advisories/aa22-137a">default credential attacks</a>.
                        </span>
            </div>
            <div class="form-input-wrapper">
              <div class="form-group">
                <label class="col-form-label pg-text" for="input_oldpass">Current Password</label>
                <input type="password" class="form-control" id="input_oldpass" name="oldpass" autocomplete="off">
              </div>
              <div class="form-group">
                <label class="col-form-label pg-text" for="input_pass1">New Password</label>
                <input type="password" class="form-control" name="pass1" id="input_pass1" autocomplete="off">
              </div>
              <div class="form-group">
                <label class="col-form-label pg-text" for="input_pass2">Confirm New Password</label>
                <input type="password" class="form-control" name="pass2" id="input_pass2" autocomplete="off">
              </div>
              <div class="form-group buttons">
                <button type="button"
                        id="btn_change_password"
                        name="btn_change_password"
                        @click="changePassword"
                        class="btn btn-primary">Change Password</button>
                <button type="button"
                        id="btn_skip"
                        name="btn_skip"
                        @click="skip"
                        class="btn btn-secondary">Skip</button>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
    <div class="" style="position: absolute; bottom: 0px; right: 10px; font-size: 3em; padding: 20pt 20pt 5pt 20pt">
      <div style="padding-bottom: 20px; padding-top: 20px">
        <Logo mode="light" class="logo-container"/>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">

.logo-container {
  width: 180px !important;
}

.login-page {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url('@/assets/wallpapers/background_dark.png');
  background-size: cover;
}

.form-content-wrapper {
  margin-left: 10%;
  padding-left: 6px;
}

.form-input-wrapper {
  max-width: 300px;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group.buttons {
  margin-top: 1.5rem;
}

.login-form {
  max-width: 480px;
}

.card {
  background-color: transparent;
  border-color: transparent;
  margin-left: 10%;
  margin-top: 10%;
}

input.form-control {
  font-size: 12px;
  padding: 10px;
  color: black;
  width: 225px;
  height: 32px;
  border-radius: 5px;
  outline: none;
  border: 2px solid rgba(97, 215, 231, 0.829);
  background-color: rgba(255, 255, 255, 0.623);
}

button {
  color: black;
  padding: 7px;
  padding-left: 28px;
  padding-right: 28px;
  font-size: 11px;
  border-radius: 30px;
  background-image: linear-gradient(to right, rgb(67, 194, 233), rgb(137, 230, 194));
  border: none;
}

button:hover {
  background-image: linear-gradient(to right, rgb(61, 168, 200), rgb(116, 187, 160));
}

.btn {
  border-radius: 1.25rem;
  color: #000;
  font-size: 11px;
}

.btn-primary, .btn-secondary {
  min-width: 80px;
}

.btn-secondary {
  margin-left: 4px;
  background-image: linear-gradient(to right, rgb(84, 142, 131), rgb(104, 156, 139));
}

.horizon {
  margin-left: 20%;
}

label.pg-text, span.pg-text {
  color: #fff;
}

a.pg-link, a.pg-link:visited {
  color: rgb(97, 184, 195);
  text-decoration: none;
}

a.pg-link:hover {
  color: rgb(90, 160, 173);
  text-decoration: underline;
}

</style>


