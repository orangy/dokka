/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import * as button from './button/index';
import * as checkbox from './checkbox/index';
import * as dropdown from './dropdown/index';
import * as filterSection from './filter-section/index';
import * as footer from './footer/index';
import * as icon from './icon/index';
import * as layout from './layout/index';
import * as libraryName from './library-name/index';
import * as libraryVersion from './library-version/index';
import * as navbar from './navbar/index';
import * as navbarButton from './navbar-button/index';
import * as platformTag from './platform-tag/index';
import * as platformTags from './platform-tags/index';
import * as tabs from './tabs/index';
import * as tocTree from './toc-tree/index';
import * as link from './link/index';
import * as breadcrumbs from './breadcrumbs/index';
import * as inlineCode from './inline-code/index';
import { removeBackwardCompatibilityStyles } from './utils';
import './helpers.scss';
import './global.scss';

export {
  button,
  checkbox,
  dropdown,
  filterSection,
  footer,
  icon,
  layout,
  libraryName,
  libraryVersion,
  navbar,
  navbarButton,
  platformTag,
  platformTags,
  tabs,
  tocTree,
  link,
  breadcrumbs,
  inlineCode,
};

document.addEventListener('DOMContentLoaded', () => {
  removeBackwardCompatibilityStyles();
});
