<template>
  <div class="container">
    <div class="login-page pc-style">
      <img :src="LogoIcon" alt="logo" class="logo-icon">
      <div class="login-tab">
        <div :class="{'tab-selected': activeTab === 'username'}" @click="setActiveTab('username')">
          <span>用户名登录</span>
        </div>
        <div :class="{'tab-selected': activeTab === 'phone'}" @click="setActiveTab('phone')">
          <span>手机号登录</span>
        </div>
        <div :class="{'tab-selected': activeTab === 'wechat'}" @click="setActiveTab('wechat')">
          <span>微信登录</span>
        </div>
      </div>

      <!-- 用户名密码登录 -->
      <div v-if="activeTab === 'username'" class="login-form">
        <div class="common-input">
          <img :src="MailIcon" class="left-icon">
          <div class="input-view">
            <input placeholder="请输入用户名" v-model="pageData.username" type="text" class="input">
          </div>
        </div>
        <div class="common-input">
          <img :src="PwdIcon" class="left-icon">
          <div class="input-view">
            <input placeholder="请输入密码" v-model="pageData.password" type="password" class="input">
          </div>
        </div>
        <button @click="handleLogin">登录</button>
      </div>

      <!-- 手机号验证码登录 -->
      <div v-if="activeTab === 'phone'" class="login-form">
        <div class="common-input">
          <img :src="PhoneIcon" class="left-icon">
          <div class="input-view">
            <input placeholder="请输入手机号" v-model="pageData.phone" type="text" class="input">
          </div>
        </div>
        <div class="common-input">
          <img :src="CodeIcon" class="left-icon">
          <div class="input-view">
            <input placeholder="请输入验证码" v-model="pageData.smsCode" type="text" class="input">
          </div>
        </div>
        <button @click="handlePhoneLogin">验证码登录</button>
      </div>

      <!-- 微信登录 -->
      <div v-if="activeTab === 'wechat'" class="login-form">
        <button @click="handleWeChatLogin">微信登录</button>
      </div>

      <div class="operation">
        <a @click="handleCreateUser" class="forget-pwd" style="text-align: left;">注册新帐号</a>
        <a class="forget-pwd" style="text-align: right;">忘记密码？</a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '../../store/modules/user'; // 相对路径导入
import { useRouter } from 'vue-router';
import { message } from "ant-design-vue";
import { ref, reactive } from 'vue';
import axios from 'axios';
import LogoIcon from '/@/assets/images/k-logo.png';
import MailIcon from '/@/assets/images/mail-icon.svg';
import PwdIcon from '/@/assets/images/pwd-icon.svg';
import PhoneIcon from '/@/assets/images/phone-icon.svg';
import CodeIcon from '/@/assets/images/code-icon.svg';

const userStore = useUserStore();
const router = useRouter();

const pageData = reactive({
  username: '',
  password: '',
  phone: '',
  smsCode: ''
});

const activeTab = ref('username'); // 控制当前登录方式

const setActiveTab = (tab: string) => {
  activeTab.value = tab;
}

const handleLogin = async () => {
  try {
    const response = await axios.post('http://localhost:9100/api/userAuth/login', {
      username: pageData.username,
      password: pageData.password
    });

    if (response.data.code === 0) {
      loginSuccess(response.data.data);
    } else {
      message.warn(response.data.message || '登录失败');
    }
  } catch (error: unknown) {
    if (error instanceof Error) {
      message.warn(error.message || '登录失败');
    } else {
      message.warn('登录失败');
    }
  }
}

const handlePhoneLogin = async () => {
  try {
    const response = await axios.post('http://localhost:9100/api/userAuth/loginByPhone', {
      phone: pageData.phone,
      smsCode: pageData.smsCode
    });

    if (response.data.code === 0) {
      loginSuccess(response.data.data);
    } else {
      message.warn(response.data.message || '登录失败');
    }
  } catch (error: unknown) {
    if (error instanceof Error) {
      message.warn(error.message || '登录失败');
    } else {
      message.warn('登录失败');
    }
  }
}

const handleWeChatLogin = async () => {
  try {
    const response = await axios.post('http://localhost:9100/api/userAuth/loginByWeChat', {
      code: 'your-wechat-code' // 这里假设你已经获取了微信的 `code`
    });

    if (response.data.code === 0) {
      loginSuccess(response.data.data);
    } else {
      message.warn(response.data.message || '登录失败');
    }
  } catch (error: unknown) {
    if (error instanceof Error) {
      message.warn(error.message || '登录失败');
    } else {
      message.warn('登录失败');
    }
  }
}

const loginSuccess = (data: any) => {
  userStore.setUserData({
    token: data.token,
    username: data.username,
    userId: data.userId
  });

  router.push({ name: 'portal' });
  message.success('登录成功！');
}

const handleCreateUser = () => {
  router.push('/register');
}
</script>


<style scoped lang="less">
div {
  display: block;
}

.container {
  //background-color: #f1f1f1;
  background-image: url('../images/admin-login-bg.jpg');
  background-size: cover;
  object-fit: cover;
  height: 100%;
  max-width: 100%;
  display:flex;
  justify-content: center;
  align-items:center;
}

.new-content {
  position: absolute;
  left: 0;
  right: 0;
  margin: 80px auto 0;
  width: 980px;
}

.logo-img {
  width: 125px;
  display: block;
  margin-left: 137.5px;
}

.login-page {
  overflow: hidden;
  background: #fff;

  .logo-icon {
    margin-top: 20px;
    margin-left: 175px;
    width: 48px;
    height: 48px;
  }
}

.pc-style {
  position: relative;
  width: 400px;
  height: 464px;
  background: #fff;
  border-radius: 4px;
  -webkit-box-shadow: 2px 2px 6px #aaa;
  box-shadow: 2px 2px 6px #aaa;
}

.login-tab {
  display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
  color: #1e1e1e;
  font-size: 14px;
  color: #1e1e1e;
  font-weight: 500;
  height: 46px;
  line-height: 44px;
  margin-bottom: 40px;
  border-bottom: 1px solid #c3c9d5;

  div {
    position: relative;
    -webkit-box-flex: 1;
    -ms-flex: 1;
    flex: 1;
    text-align: center;
    cursor: pointer;
  }

  .tabline {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    margin: 0 auto;
    display: inline-block;
    width: 0;
    height: 2px;
    background: #3d5b96;
    -webkit-transition: width .5s cubic-bezier(.46, 1, .23, 1.52);
    transition: width .5s cubic-bezier(.46, 1, .23, 1.52);
  }

  tab-selected {
    color: #1e1e1e;
    font-weight: 500;
  }

  .mail-login, .tel-login {
    padding: 0 28px;
  }

}

.mail-login {
  margin: 0px 24px;
}

.common-input {
  display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
  -webkit-box-align: start;
  -ms-flex-align: start;
  align-items: flex-start;

  .left-icon {
    margin-right: 12px;
    width: 24px;
  }

  .input-view {
    -webkit-box-flex: 1;
    -ms-flex: 1;
    flex: 1;

    .input {
      font-weight: 500;
      font-size: 14px;
      color: #1e1e1e;
      height: 26px;
      line-height: 26px;
      border: none;
      padding: 0;
      display: block;
      width: 100%;
      letter-spacing: 1.5px;
    }

    err-view {
      margin-top: 4px;
      height: 16px;
      line-height: 16px;
      font-size: 12px;
      color: #f62a2a;
    }
  }
}

.next-btn {
  background: #3d5b96;
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  height: 40px;
  line-height: 40px;
  text-align: center;
  width: 100%;
  outline: none;
  cursor: pointer;
}

button {
  background: transparent;
  padding: 0;
  border-width: 0px;
}

button, input, select, textarea {
  margin: 0;
  padding: 0;
  outline: none;
}

.operation {
  display: flex;
  flex-direction: row;
  margin: 0 24px;
}

.forget-pwd {
  //text-align: center;
  display: block;
  overflow: hidden;
  flex:1;
  margin: 0 auto;
  color: #3d5b96;
  font-size: 14px;
  cursor: pointer;
}
</style>
