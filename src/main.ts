import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './assets/styles/main.scss'
import { AMapConfig } from './config/amap'

// 异步加载高德地图API
function loadAMapAPI() {
  return new Promise((resolve, reject) => {
    if ((window as any).AMap) {
      console.log('高德地图API已加载');
      resolve((window as any).AMap);
      return;
    }
    
    (window as any)._AMapSecurityConfig = {
      securityJsCode: AMapConfig.securityKey,
    };

    const script = document.createElement('script');
    script.src = `https://webapi.amap.com/maps?v=${AMapConfig.version}&key=${AMapConfig.apiKey}&plugin=AMap.Scale,AMap.ToolBar,AMap.Geolocation,AMap.HeatMap`;
    script.onload = () => {
      console.log('高德地图API加载成功');
      resolve((window as any).AMap);
    };
    script.onerror = (err) => {
      console.error('高德地图API加载失败', err);
      reject(err);
    };
    document.head.appendChild(script);
  });
}

const app = createApp(App)

// 全局注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus)
app.use(router)
app.use(pinia)

// 先加载API，再挂载应用
loadAMapAPI().then(() => {
  app.mount('#app');
}).catch(err => {
  console.error('无法启动应用，因为高德地图API加载失败', err);
  // 可以在这里显示一个全局的错误提示
});
