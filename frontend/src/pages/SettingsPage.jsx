import React from 'react';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/useAuthStore';
import { toast } from 'react-hot-toast';

function SettingsPage() {
  const { t, i18n } = useTranslation();
  const lan = i18n.language;
  const { connectSocket, socketConnected } = useAuthStore();

  const handleLanguageChange = async (newLang) => {
    try {
      await i18n.changeLanguage(newLang);
      
      // 检查WebSocket连接状态
      if (!socketConnected) {
        console.log('Attempting to reconnect WebSocket after language change...');
        connectSocket();
      }
    } catch (error) {
      console.error('Error changing language:', error);
      toast.error('Failed to change language');
    }
  };

  return (
    <div className="flex w-full bg-red-primary relative h-[100vh] items-center justify-center">
      <div className="h-1/2 w-1/2 bg-base-300 p-10 rounded-2xl flex flex-col">
        <div className="text-center text-2xl"><h1>{t('settings')}</h1></div>
        <div className='container w-full flex gap-4 mt-4'>
          <div>{t('language')}</div>
          <div>
            <label className="label mx-2" htmlFor='zh'>
              <span className="label-text">中文</span>
            </label>
            <input id='zh' type="radio" name="language" className="radio" 
                  checked={lan === 'zh'} onChange={() => handleLanguageChange('zh')} />
          </div>
          <div>
            <label className="label mx-2" htmlFor='en'>
              <span className="label-text">English</span>
            </label>
            <input id='en' type="radio" name="language" className="radio" 
             checked={lan === 'en'} onChange={() => handleLanguageChange('en')} />
          </div>
        </div>
      </div>
    </div>
  )
}

export default SettingsPage