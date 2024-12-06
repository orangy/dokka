/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import './styles.scss';

function initButton() {
  const buttons = document.querySelectorAll('div.button');
  buttons.forEach((button: Element) => {
    button.addEventListener('keyup', (event) => {
      const key = (event as KeyboardEvent).key;
      if (key === 'Enter' || key === ' ') {
        button.dispatchEvent(new MouseEvent('click'));
      }
    });
  });
}

document.addEventListener('DOMContentLoaded', () => {
  initButton();
});
