/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.pages

interface MultimoduleRootPage : ContentPage

interface ModulePage : ContentPage, WithDocumentables

interface PackagePage : ContentPage, WithDocumentables

interface ClasslikePage : ContentPage, WithDocumentables

interface MemberPage : ContentPage, WithDocumentables
