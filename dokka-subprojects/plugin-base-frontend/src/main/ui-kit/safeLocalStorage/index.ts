/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

/** When Dokka is viewed via iframe, local storage could be inaccessible (see https://github.com/Kotlin/dokka/issues/3323)
 * This is a wrapper around local storage to prevent errors in such cases
 * */
export const safeLocalStorage = (() => {
  let isLocalStorageAvailable = false;
  try {
    const testKey = '__testLocalStorageKey__';
    localStorage.setItem(testKey, testKey);
    localStorage.removeItem(testKey);
    isLocalStorageAvailable = true;
  } catch (e) {
    console.error('Local storage is not available', e);
  }

  return {
    getItem: (key: string) => {
      if (!isLocalStorageAvailable) {
        return null;
      }
      return localStorage.getItem(key);
    },
    setItem: (key: string, value: string) => {
      if (!isLocalStorageAvailable) {
        return;
      }
      localStorage.setItem(key, value);
    },
  };
})();
