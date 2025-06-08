import './utils/styles/main.scss'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router.js'

const app = createApp(App)
app.use(ElementPlus)
app.use(createPinia())
app.use(router)
app.config.errorHandler = (err, vm, info) => {
  const message = `vue逻辑报错: ${vm.$.vnode.type.__file} 发生错误：${err} 所在生命周期：${info}`;
  console.error(message);
};

app.mount('#app')
