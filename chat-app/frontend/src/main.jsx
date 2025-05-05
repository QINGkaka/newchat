import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import './global.css'

import {BrowserRouter} from 'react-router-dom';

createRoot(document.getElementById('root')).render(
  //StrictMode 的**双重调用**是一种主动防御式编程策略，通过模拟极端场景（如并发渲染的重复执行），
  // 强制开发者提前修复问题，确保代码更健壮、兼容未来 React 版本。
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
