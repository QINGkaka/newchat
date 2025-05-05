import React from 'react'
import { useState } from 'react'
import { useAuthStore } from '../store/useAuthStore';
import { Eye, EyeOff, Loader2, Lock, Mail, MessageSquare, User } from "lucide-react";
import { Link } from "react-router-dom";
import AuthImagePattern from "../components/AuthImagePattern.jsx";
import {toast} from 'react-hot-toast';
import { useTranslation } from 'react-i18next';

function SignUpPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
  });

  const {isSigningUp, signup} = useAuthStore();

  const { t } = useTranslation();

  const validateForm = () => {
    const errors = [];
    
    if (!formData.fullName.trim()) {
      errors.push(t('fullNameRequired'));
    }
    
    if (!formData.email.trim()) {
      errors.push(t('emailRequired'));
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      errors.push(t('invalidEmailFormat'));
    }
    
    if (!formData.password) {
      errors.push(t('passwordRequired'));
    } else {
      if (formData.password.length < 8) {
        errors.push(t('passwordMinLength'));
      }
      if (!/[A-Z]/.test(formData.password)) {
        errors.push(t('passwordUppercaseRequired'));
      }
      if (!/[a-z]/.test(formData.password)) {
        errors.push(t('passwordLowercaseRequired'));
      }
      if (!/[0-9]/.test(formData.password)) {
        errors.push(t('passwordNumberRequired'));
      }
    }

    if (errors.length > 0) {
      errors.forEach(error => toast.error(error));
      return false;
    }

    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (validateForm()) {
      try {
        await signup(formData);
      } catch (error) {
        console.error('Signup error:', error);
      }
    }
  };
  
  return (
    <div className="min-h-screen grid lg:grid-cols-2">
      {/* left side */}
      <div className="flex flex-col justify-center items-center p-6 sm:p-12">
        <div className="w-full max-w-md space-y-8">
          {/* LOGO */}
          <div className="text-center mb-8">
            <div className="flex flex-col items-center gap-2 group">
              <div
                className="size-12 rounded-xl bg-primary/10 flex items-center justify-center 
              group-hover:bg-primary/20 transition-colors"
              >
                <MessageSquare className="size-6 text-primary" />
              </div>
              <h1 className="text-2xl font-bold mt-2">{t('createAccount')}</h1>
              <p className="text-base-content/60">{t('getStartedWithYourFreeAccount')}</p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="form-control">
              <label className="label">
                <span className="label-text font-medium">{t('fullName')}</span>
              </label>
              <div className="relative">
                <div className='absolute z-100 left-2 top-2'>
                  <User className="size-5 text-base-content/40"/>
                </div>
                <input
                  type="text"
                  className={`input input-bordered w-full pl-10`}
                  placeholder="John Doe"
                  value={formData.fullName}
                  onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                />
              </div>
            </div>

            <div className="form-control">
              <label className="label">
                <span className="label-text font-medium">{t('email')}</span>
              </label>
              <div className="relative">
                <div className="absolute z-100 left-2 top-2">
                  <Mail  className="size-5 text-base-content/40"/>
                </div>
                <input
                  type="email"
                  className={`input input-bordered w-full pl-10`}
                  placeholder="you@example.com"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
              </div>
            </div>

            <div className="form-control">
              <label className="label">
                <span className="label-text font-medium">{t('password')}</span>
              </label>
              <div className="relative">
                <div className="absolute z-100 left-2 top-2">
                  <Lock className="size-5 text-base-content/40" />
                </div>
                <input
                  type={showPassword ? "text" : "password"}
                  className={`input input-bordered w-full pl-10`}
                  placeholder="••••••••"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                />
                <button
                  type="button"
                  className="absolute inset-y-0 right-0 pr-3 flex items-center"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? (
                    <EyeOff className="size-5 text-base-content/40" />
                  ) : (
                    <Eye className="size-5 text-base-content/40" />
                  )}
                </button>
              </div>
            </div>

            <button type="submit" className="btn btn-primary w-full" disabled={isSigningUp}>
              {isSigningUp ? (
                <>
                  <Loader2 className="size-5 animate-spin" />
                  Loading...
                </>
              ) : (
                t("createAccount")
              )}
            </button>
          </form>

          <div className="text-center">
            <p className="text-base-content/60">
              {t('alreadyHaveAnAccount')} &nbsp; &nbsp;
              <Link to="/login" className="link link-primary">
                {t('signIn')}
              </Link>
            </p>
          </div>
        </div>
      </div>

      {/* right side */}

      <AuthImagePattern
        title={t('joinOurCommunity')}
        subtitle={t('connectShareTouch')}
      />
    </div>
  )
}

export default SignUpPage