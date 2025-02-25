/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import { ColumnResizer } from '@column-resizer/core';
import { safeLocalStorage } from '../safeLocalStorage';
import { getScreenType } from '../utils';
import './styles.scss';

const CONTAINER_ELEMENT_ID = 'container';
const SIDEBAR_ELEMENT_ID = 'leftColumn';
const SIDEBAR_WIDTH_KEY = 'DOKKA_SIDEBAR_WIDTH';
const DEFAULT_SIDEBAR_WIDTH = 280;

document.addEventListener('DOMContentLoaded', () => {
  let currentScreenType = getScreenType();
  const columnResizer = new ColumnResizer({ vertical: false });
  const containerElement = document.getElementById(CONTAINER_ELEMENT_ID);
  if (containerElement) {
    if (currentScreenType === 'desktop') {
      enableColumnResizer(containerElement, columnResizer);
    }

    const resizeObserver = new ResizeObserver(() => {
      const nextScreenType = getScreenType();
      if (nextScreenType !== currentScreenType) {
        if (nextScreenType === 'desktop') {
          enableColumnResizer(containerElement, columnResizer);
        } else {
          disableColumnResizer(containerElement, columnResizer);
        }
      }
      currentScreenType = nextScreenType;
    });
    resizeObserver.observe(containerElement);
  }
});

function enableColumnResizer(containerElement: HTMLElement, columnResizer: ColumnResizer) {
  restoreSidebarWidth();
  columnResizer.init(containerElement);
  columnResizer.on(containerElement, 'column:after-resizing', storeSidebarWidth);
}

function disableColumnResizer(containerElement: HTMLElement, columnResizer: ColumnResizer) {
  columnResizer.dispose();
  // Remove inline styles added by column resizer
  containerElement.querySelectorAll('[data-item-type]').forEach((child) => {
    child.removeAttribute('style');
  });
}

function restoreSidebarWidth() {
  const storedSidebarWidth = safeLocalStorage.getItem(SIDEBAR_WIDTH_KEY);
  if (storedSidebarWidth) {
    const sidebar = document.getElementById(SIDEBAR_ELEMENT_ID);
    if (sidebar) {
      updateSidebarConfig(sidebar, Number(storedSidebarWidth));
    }
  }
}

function storeSidebarWidth() {
  const sidebar = document.getElementById(SIDEBAR_ELEMENT_ID);
  const currentSidebarWidth = sidebar ? sidebar.offsetWidth : DEFAULT_SIDEBAR_WIDTH;
  if (sidebar) {
    updateSidebarConfig(sidebar, currentSidebarWidth);
  }
  safeLocalStorage.setItem(SIDEBAR_WIDTH_KEY, `${currentSidebarWidth}`);
}

function updateSidebarConfig(sidebar: HTMLElement, width: number) {
  const previousConfig = JSON.parse(sidebar.getAttribute('data-item-config') || '{}');
  sidebar.setAttribute('data-item-config', JSON.stringify({ ...previousConfig, defaultSize: width }));
}
