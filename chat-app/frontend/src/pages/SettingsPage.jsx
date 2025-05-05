import React from 'react';
import { useTranslation } from 'react-i18next';

function SettingsPage() {
  const { t, i18n } = useTranslation();
  const lan = i18n.language;
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
                  checked={lan === 'zh'} onChange={() => i18n.changeLanguage('zh')} />
          </div>
          <div>
            <label className="label mx-2" htmlFor='en'>
              <span className="label-text">English</span>
            </label>
            <input id='en' type="radio" name="language" className="radio" 
             checked={lan === 'en'} onChange={() => i18n.changeLanguage('en')} />
          </div>
        </div>
      </div>
    </div>
  )
}

export default SettingsPage