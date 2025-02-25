<#import "includes/page_metadata.ftl" as page_metadata>
<#import "includes/header.ftl" as header>
<#import "includes/footer.ftl" as footer>
<!DOCTYPE html>
<html class="no-js">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1" charset="UTF-8">
    <@page_metadata.display/>
    <@template_cmd name="pathToRoot">
        <script>var pathToRoot = "${pathToRoot}";</script></@template_cmd>
    <script>document.documentElement.classList.replace("no-js", "js");</script>
    <#-- This script doesn't need to be there but it is nice to have
    since app in dark mode doesn't 'blink' (class is added before it is rendered) -->
    <script>const storage = localStorage.getItem("dokka-dark-mode")
      if (storage == null) {
        const osDarkSchemePreferred = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
        if (osDarkSchemePreferred === true) {
          document.getElementsByTagName("html")[0].classList.add("theme-dark")
        }
      } else {
        const savedDarkMode = JSON.parse(storage)
        if (savedDarkMode === true) {
          document.getElementsByTagName("html")[0].classList.add("theme-dark")
        }
      }
    </script>
    <#-- Resources (scripts, stylesheets) are handled by Dokka.
    Use customStyleSheets and customAssets to change them. -->
    <@resources/>
</head>
<body>
<div class="root">
    <@header.display/>
    <div id="container">
        <div id="leftColumn" class="sidebar" data-item-type="SECTION" data-item-config='{"defaultSize": 280, "minSize": 200, "maxSize": 400}'>
            <div class="dropdown theme-dark_mobile" data-role="dropdown" id="toc-dropdown">
                <ul role="listbox" id="toc-listbox" class="dropdown--list dropdown--list_toc-list"
                    data-role="dropdown-listbox">
                    <div class="dropdown--header">
                            <span>
                                <@template_cmd name="projectName">
                                    ${projectName}
                                </@template_cmd>
                            </span>
                        <button class="button" data-role="dropdown-toggle" aria-label="Close table of contents">
                            <i class="ui-kit-icon ui-kit-icon_cross"></i>
                        </button>
                    </div>
                    <div class="sidebar--inner" id="sideMenu"></div>
                </ul>
                <div class="dropdown--overlay"></div>
            </div>
        </div>
        <div id="resizer" class="resizer" data-item-type="BAR"></div>
        <div id="main" data-item-type="SECTION">
            <@content/>
            <@footer.display/>
        </div>
    </div>
</div>
</body>
</html>