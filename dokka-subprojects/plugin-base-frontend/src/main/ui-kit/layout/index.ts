/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import { ColumnResizer } from '@column-resizer/core';
import './styles.scss';
import { safeLocalStorage } from '../safeLocalStorage';

const SIDEBAR_WIDTH_KEY = 'sidebarWidth';
const DEFAULT_SIDEBAR_WIDTH = 280;

document.addEventListener('DOMContentLoaded', () => {
  const maybeSidebarWidth = safeLocalStorage.getItem(SIDEBAR_WIDTH_KEY);
  if (maybeSidebarWidth) {
    const sidebar = document.getElementById('leftColumn');
    if (sidebar) {
      sidebar.setAttribute('data-item-config', JSON.stringify({ defaultSize: maybeSidebarWidth }));
    }
  }
  const columnResizer = new ColumnResizer({ vertical: false });
  const containerElement = document.getElementById('container');
  setTimeout(() => {
    columnResizer.init(containerElement);
  }, 0);

  columnResizer.on(containerElement, 'column:after-resizing', () => {
    const sidebar = document.getElementById('leftColumn');
    const sidebarWidth = sidebar ? sidebar.offsetWidth : DEFAULT_SIDEBAR_WIDTH;
    if (sidebar) {
      sidebar.setAttribute('data-item-config', JSON.stringify({ defaultSize: sidebarWidth }));
    }
    safeLocalStorage.setItem(SIDEBAR_WIDTH_KEY, sidebarWidth.toString());
  });
});
