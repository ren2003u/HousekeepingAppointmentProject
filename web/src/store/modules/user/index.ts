import { defineStore } from 'pinia';


// store/modules/user/index.ts

// store/modules/user/index.ts


export const useUserStore = defineStore('user', {
  state: () => ({
    user_id: undefined as string | undefined,   // 添加类型注解
    user_name: undefined as string | undefined, // 添加类型注解
    user_token: undefined as string | undefined // 添加类型注解
  }),
  actions: {
    setUserData(data: { token: string, username: string, userId: string }) {
      this.user_token = data.token;
      this.user_name = data.username;
      this.user_id = data.userId;
    },
    clearUserData() {
      this.user_token = undefined;
      this.user_name = undefined;
      this.user_id = undefined;
    }
  }
});

