var BitwardenExtension = function () { };

BitwardenExtension.prototype = {
    run: function (arguments) {
        console.log('Run');
        console.log(arguments);

        var args = {
            'url_string': document.URL,
            pageDetails: this.collect(document)
        };

        console.log(args);
        arguments.completionFunction(args);
    },
    finalize: function (arguments) {
        console.log('Finalize');
        console.log(arguments);

        if (arguments.fillScript) {
            this.fill(document, JSON.parse(arguments.fillScript));
        }
    },

    /*
    1Password Extension

    Lovingly handcrafted by Dave Teare, Michael Fey, Rad Azzouz, and Roustem Karimov.
    Copyright (c) 2014 AgileBits. All rights reserved.

    ================================================================================

    Copyright (c) 2014 AgileBits Inc.

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
    */

    collect: function(document, undefined) {
        var isFirefox = false, isChrome = false, isSafari = true;
        document.elementsByOPID={};document.addEventListener('input',function(b){!1!==b.a&&'input'===b.target.tagName.toLowerCase()&&(b.target.dataset['com.agilebits.onepassword.userEdited']='yes')},!0);
        function q(b,d){function f(a,e){var c=a[e];if('string'==typeof c)return c;c=a.getAttribute(e);return'string'==typeof c?c:null}function h(a,e){if(-1===['text','password'].indexOf(e.type.toLowerCase())||!(m.test(a.value)||m.test(a.htmlID)||m.test(a.htmlName)||m.test(a.placeholder)||m.test(a['label-tag'])||m.test(a['label-data'])||m.test(a['label-aria'])))return!1;if(!a.visible)return!0;if('password'==e.type.toLowerCase())return!1;var c=e.type;v(e,!0);return c!==e.type}function n(a){switch(p(a.type)){case 'checkbox':return a.checked?
        '✓':'';case 'hidden':a=a.value;if(!a||'number'!=typeof a.length)return'';254<a.length&&(a=a.substr(0,254)+'...SNIPPED');return a;default:return a.value}}function l(a){return a.options?(a=Array.prototype.slice.call(a.options).map(function(a){var c=a.text,c=c?p(c).replace(/\\s/mg,'').replace(/[~`!@$%^&*()\\-_+=:;'\"\\[\\]|\\\\,<.>\\?]/mg,''):null;return[c?c:null,a.value]}),{options:a}):null}function r(a){var e;for(a=a.parentElement||a.parentNode;a&&'td'!=p(a.tagName);)a=a.parentElement||a.parentNode;if(!a||
        void 0===a)return null;e=a.parentElement||a.parentNode;if('tr'!=e.tagName.toLowerCase())return null;e=e.previousElementSibling;if(!e||'tr'!=(e.tagName+'').toLowerCase()||e.cells&&a.cellIndex>=e.cells.length)return null;a=e.cells[a.cellIndex];a=a.textContent||a.innerText;return a=x(a)}function s(a){var e,c=[];if(a.labels&&a.labels.length&&0<a.labels.length)c=Array.prototype.slice.call(a.labels);else{a.id&&(c=c.concat(Array.prototype.slice.call(w(b,'label[for='+JSON.stringify(a.id)+']'))));if(a.name){e=
        w(b,'label[for='+JSON.stringify(a.name)+']');for(var f=0;f<e.length;f++)-1===c.indexOf(e[f])&&c.push(e[f])}for(e=a;e&&e!=b;e=e.parentNode)'label'===p(e.tagName)&&-1===c.indexOf(e)&&c.push(e)}0===c.length&&(e=a.parentNode,'dd'===e.tagName.toLowerCase()&&null!==e.previousElementSibling&&'dt'===e.previousElementSibling.tagName.toLowerCase()&&c.push(e.previousElementSibling));return 0<c.length?c.map(function(a){return(a.textContent||a.innerText).replace(/^\\s+/,'').replace(/\\s+$/,'').replace('\\n','').replace(/\\s{2,}/,
        ' ')}).join(''):null}function g(a,e,c,b){void 0!==b&&b===c||null===c||void 0===c||(a[e]=c)}function p(a){return'string'===typeof a?a.toLowerCase():(''+a).toLowerCase()}function w(a,b){var c=[];try{c=a.querySelectorAll(b)}catch(f){}return c}var t=b.defaultView?b.defaultView:window,m=RegExp('((\\\\b|_|-)pin(\\\\b|_|-)|password|passwort|kennwort|(\\\\b|_|-)passe(\\\\b|_|-)|contraseña|senha|密码|adgangskode|hasło|wachtwoord)','i'),u=Array.prototype.slice.call(w(b,'form')).map(function(a,b){var c={},d='__form__'+
        b;a.opid=d;c.opid=d;g(c,'htmlName',f(a,'name'));g(c,'htmlID',f(a,'id'));d=f(a,'action');d=new URL(d,window.location.href);g(c,'htmlAction',d?d.href:null);g(c,'htmlMethod',f(a,'method'));return c}),E=Array.prototype.slice.call(y(b)).map(function(a,e){var c={},d='__'+e,k=-1==a.maxLength?999:a.maxLength;if(!k||'number'===typeof k&&isNaN(k))k=999;b.elementsByOPID[d]=a;a.opid=d;c.opid=d;c.elementNumber=e;g(c,'maxLength',Math.min(k,999),999);c.visible=z(a);c.viewable=A(a);g(c,'htmlID',f(a,'id'));g(c,'htmlName',
        f(a,'name'));g(c,'htmlClass',f(a,'class'));g(c,'tabindex',f(a,'tabindex'));g(c,'title',f(a,'title'));g(c,'userEdited',!!a.dataset['com.agilebits.onepassword.userEdited']);if('hidden'!=p(a.type)){g(c,'label-tag',s(a));g(c,'label-data',f(a,'data-label'));g(c,'label-aria',f(a,'aria-label'));g(c,'label-top',r(a));d=[];for(k=a;k&&k.nextSibling;){k=k.nextSibling;if(B(k))break;C(d,k)}g(c,'label-right',d.join(''));d=[];D(a,d);d=d.reverse().join('');g(c,'label-left',d);g(c,'placeholder',f(a,'placeholder'))}g(c,
        'rel',f(a,'rel'));g(c,'type',p(f(a,'type')));g(c,'value',n(a));g(c,'checked',a.checked,!1);g(c,'autoCompleteType',a.getAttribute('x-autocompletetype')||a.getAttribute('autocompletetype')||a.getAttribute('autocomplete'),'off');g(c,'disabled',a.disabled);g(c,'readonly',a.b||a.readOnly);g(c,'selectInfo',l(a));g(c,'aria-hidden','true'==a.getAttribute('aria-hidden'),!1);g(c,'aria-disabled','true'==a.getAttribute('aria-disabled'),!1);g(c,'aria-haspopup','true'==a.getAttribute('aria-haspopup'),!1);g(c,'data-unmasked',
        a.dataset.unmasked);g(c,'data-stripe',f(a,'data-stripe'));g(c,'onepasswordFieldType',a.dataset.onepasswordFieldType||a.type);g(c,'onepasswordDesignation',a.dataset.onepasswordDesignation);g(c,'onepasswordSignInUrl',a.dataset.onepasswordSignInUrl);g(c,'onepasswordSectionTitle',a.dataset.onepasswordSectionTitle);g(c,'onepasswordSectionFieldKind',a.dataset.onepasswordSectionFieldKind);g(c,'onepasswordSectionFieldTitle',a.dataset.onepasswordSectionFieldTitle);g(c,'onepasswordSectionFieldValue',a.dataset.onepasswordSectionFieldValue);
        a.form&&(c.form=f(a.form,'opid'));g(c,'fakeTested',h(c,a),!1);return c});E.filter(function(a){return a.fakeTested}).forEach(function(a){var e=b.elementsByOPID[a.opid];e.getBoundingClientRect();var c=e.value;!e||e&&'function'!==typeof e.click||e.click();v(e,!1);e.dispatchEvent(F(e,'keydown'));e.dispatchEvent(F(e,'keypress'));e.dispatchEvent(F(e,'keyup'));e.value!==c&&(e.value=c);e.click&&e.click();a.postFakeTestVisible=z(e);a.postFakeTestViewable=A(e);a.postFakeTestType=e.type;a=e.value;var c=e.ownerDocument.createEvent('HTMLEvents'),
        d=e.ownerDocument.createEvent('HTMLEvents');e.dispatchEvent(F(e,'keydown'));e.dispatchEvent(F(e,'keypress'));e.dispatchEvent(F(e,'keyup'));d.initEvent('input',!0,!0);e.dispatchEvent(d);c.initEvent('change',!0,!0);e.dispatchEvent(c);e.blur();e.value!==a&&(e.value=a)});t={documentUUID:d,title:b.title,url:t.location.href,documentUrl:b.location.href,tabUrl:t.location.href,forms:function(a){var b={};a.forEach(function(a){b[a.opid]=a});return b}(u),fields:E,collectedTimestamp:(new Date).getTime()};(u=document.querySelector('[data-onepassword-title]'))&&
        u.dataset[DISPLAY_TITLE_ATTRIBUE]&&(t.displayTitle=u.dataset.onepasswordTitle);return t};document.elementForOPID=G;function F(b,d){var f;isFirefox?(f=document.createEvent('KeyboardEvent'),f.initKeyEvent(d,!0,!1,null,!1,!1,!1,!1,0,0)):(f=b.ownerDocument.createEvent('Events'),f.initEvent(d,!0,!1),f.charCode=0,f.keyCode=0,f.which=0,f.srcElement=b,f.target=b);return f}window.LOGIN_TITLES=[/^\\W*log\\W*[oi]n\\W*$/i,/log\\W*[oi]n (?:securely|now)/i,/^\\W*sign\\W*[oi]n\\W*$/i,'continue','submit','weiter','accès','вход','connexion','entrar','anmelden','accedi','valider','登录','लॉग इन करें','change password'];
        window.LOGIN_RED_HERRING_TITLES=['already have an account','sign in with'];window.REGISTER_TITLES='register;sign up;signup;join;create my account;регистрация;inscription;regístrate;cadastre-se;registrieren;registrazione;注册;साइन अप करें'.split(';');window.SEARCH_TITLES='search find поиск найти искать recherche suchen buscar suche ricerca procurar 検索'.split(' ');window.FORGOT_PASSWORD_TITLES='forgot geändert vergessen hilfe changeemail español'.split(' ');
        window.REMEMBER_ME_TITLES=['remember me','rememberme','keep me signed in'];window.BACK_TITLES=['back','назад'];function x(b){var d=null;b&&(d=b.replace(/^\\s+|\\s+$|\\r?\\n.*$/mg,''),d=0<d.length?d:null);return d}function C(b,d){var f;f='';3===d.nodeType?f=d.nodeValue:1===d.nodeType&&(f=d.textContent||d.innerText);(f=x(f))&&b.push(f)}
        function B(b){var d;b&&void 0!==b?(d='select option input form textarea button table iframe body head script'.split(' '),b?(b=b?(b.tagName||'').toLowerCase():'',d=d.constructor==Array?0<=d.indexOf(b):b===d):d=!1):d=!0;return d}
        function D(b,d,f){var h;for(f||(f=0);b&&b.previousSibling;){b=b.previousSibling;if(B(b))return;C(d,b)}if(b&&0===d.length){for(h=null;!h;){b=b.parentElement||b.parentNode;if(!b)return;for(h=b.previousSibling;h&&!B(h)&&h.lastChild;)h=h.lastChild}B(h)||(C(d,h),0===d.length&&D(h,d,f+1))}}
        function z(b){var d=b;b=(b=b.ownerDocument)?b.defaultView:{};for(var f;d&&d!==document;){f=b.getComputedStyle?b.getComputedStyle(d,null):d.style;if(!f)return!0;if('none'===f.display||'hidden'==f.visibility)return!1;d=d.parentNode}return d===document}
        function A(b){var d=b.ownerDocument.documentElement,f=b.getBoundingClientRect(),h=d.scrollWidth,n=d.scrollHeight,l=f.left-d.clientLeft,d=f.top-d.clientTop,r;if(!z(b)||!b.offsetParent||10>b.clientWidth||10>b.clientHeight)return!1;var s=b.getClientRects();if(0===s.length)return!1;for(var g=0;g<s.length;g++)if(r=s[g],r.left>h||0>r.right)return!1;if(0>l||l>h||0>d||d>n)return!1;for(f=b.ownerDocument.elementFromPoint(l+(f.right>window.innerWidth?(window.innerWidth-l)/2:f.width/2),d+(f.bottom>window.innerHeight?
        (window.innerHeight-d)/2:f.height/2));f&&f!==b&&f!==document;){if(f.tagName&&'string'===typeof f.tagName&&'label'===f.tagName.toLowerCase()&&b.labels&&0<b.labels.length)return 0<=Array.prototype.slice.call(b.labels).indexOf(f);f=f.parentNode}return f===b}
        function G(b){var d;if(void 0===b||null===b)return null;try{var f=Array.prototype.slice.call(y(document)),h=f.filter(function(d){return d.opid==b});if(0<h.length)d=h[0],1<h.length&&console.warn('More than one element found with opid '+b);else{var n=parseInt(b.split('__')[1],10);isNaN(n)||(d=f[n])}}catch(l){console.error('An unexpected error occurred: '+l)}finally{return d}};function y(b){var d=[];try{d=b.querySelectorAll('input, select, button')}catch(f){}return d}function v(b,d){if(d){var f=b.value;b.focus();b.value!==f&&(b.value=f)}else b.focus()};
        return JSON.stringify(q(document, 'oneshotUUID'));
    },
    fill: function(document, fillScript, undefined) {
        var isFirefox = false, isChrome = false, isSafari = true;
        var g=!0,k=!0;
        function n(a){var b=null;return a?0===a.indexOf('https://')&&'http:'===document.location.protocol&&(b=document.querySelectorAll('input[type=password]'),0<b.length&&(confirmResult=confirm('1Password warning: This is an unsecured HTTP page, and any information you submit can potentially be seen and changed by others. This Login was originally saved on a secure (HTTPS) page.\\n\\nDo you still wish to fill this login?'),0==confirmResult))?!0:!1:!1}
        function m(a){var b,c=[],d=a.properties,e=1,h,f=[];d&&d.delay_between_operations&&(e=d.delay_between_operations);if(!n(a.savedURL)){h=function(a,b){var d=a[0];if(void 0===d)b();else{if('delay'===d.operation||'delay'===d[0])e=d.parameters?d.parameters[0]:d[1];else{if(d=p(d))for(var l=0;l<d.length;l++)-1===f.indexOf(d[l])&&f.push(d[l]);c=c.concat(f.map(function(a){return a&&a.hasOwnProperty('opid')?a.opid:null}))}setTimeout(function(){h(a.slice(1),b)},e)}};if(b=a.options)b.hasOwnProperty('animate')&&
        (k=b.animate),b.hasOwnProperty('markFilling')&&(g=b.markFilling);a.itemType&&'fillPassword'===a.itemType&&(g=!1);a.hasOwnProperty('script')&&(b=a.script,h(b,function(){a.hasOwnProperty('autosubmit')&&'function'==typeof autosubmit&&(a.itemType&&'fillLogin'!==a.itemType||(0<f.length?setTimeout(function(){autosubmit(a.autosubmit,d.allow_clicky_autosubmit,f)},AUTOSUBMIT_DELAY):DEBUG_AUTOSUBMIT&&console.log('[AUTOSUBMIT] Not attempting to submit since no fields were filled: ',f)));'object'==typeof protectedGlobalPage&&
        protectedGlobalPage.b('fillItemResults',{documentUUID:documentUUID,fillContextIdentifier:a.fillContextIdentifier,usedOpids:c},function(){fillingItemType=null})}))}}var x={fill_by_opid:q,fill_by_query:r,click_on_opid:s,click_on_query:t,touch_all_fields:u,simple_set_value_by_query:v,focus_by_opid:w,delay:null};
        function p(a){var b;if(a.hasOwnProperty('operation')&&a.hasOwnProperty('parameters'))b=a.operation,a=a.parameters;else if('[object Array]'===Object.prototype.toString.call(a))b=a[0],a=a.splice(1);else return null;return x.hasOwnProperty(b)?x[b].apply(this,a):null}function q(a,b){var c;return(c=y(a))?(z(c,b),[c]):null}function r(a,b){var c;c=A(a);return Array.prototype.map.call(Array.prototype.slice.call(c),function(a){z(a,b);return a},this)}
        function v(a,b){var c,d=[];c=A(a);Array.prototype.forEach.call(Array.prototype.slice.call(c),function(a){a.disabled||a.a||a.readOnly||void 0===a.value||(a.value=b,d.push(a))});return d}function w(a){if(a=y(a))'function'===typeof a.click&&a.click(),'function'===typeof a.focus&&B(a,!0);return null}function s(a){return(a=y(a))?C(a)?[a]:null:null}
        function t(a){a=A(a);return Array.prototype.map.call(Array.prototype.slice.call(a),function(a){C(a);'function'===typeof a.click&&a.click();'function'===typeof a.focus&&B(a,!0);return[a]},this)}function u(){D()};var E={'true':!0,y:!0,1:!0,yes:!0,'✓':!0},F=200;function z(a,b){var c;if(a&&null!==b&&void 0!==b&&!(a.disabled||a.a||a.readOnly))switch(g&&a.form&&!a.form.opfilled&&(a.form.opfilled=!0),a.type?a.type.toLowerCase():null){case 'checkbox':c=b&&1<=b.length&&E.hasOwnProperty(b.toLowerCase())&&!0===E[b.toLowerCase()];a.checked===c||G(a,function(a){a.checked=c});break;case 'radio':!0===E[b.toLowerCase()]&&a.click();break;default:a.value==b||G(a,function(a){a.value=b})}}
        function G(a,b){H(a);b(a);I(a);J(a)&&(a.className+=' com-agilebits-onepassword-extension-animated-fill',setTimeout(function(){a&&a.className&&(a.className=a.className.replace(/(\\s)?com-agilebits-onepassword-extension-animated-fill/,''))},F))};document.elementForOPID=y;function K(a,b){var c;isFirefox?(c=document.createEvent('KeyboardEvent'),c.initKeyEvent(b,!0,!1,null,!1,!1,!1,!1,0,0)):(c=a.ownerDocument.createEvent('Events'),c.initEvent(b,!0,!1),c.charCode=0,c.keyCode=0,c.which=0,c.srcElement=a,c.target=a);return c}function H(a){var b=a.value;C(a);B(a,!1);a.dispatchEvent(K(a,'keydown'));a.dispatchEvent(K(a,'keypress'));a.dispatchEvent(K(a,'keyup'));a.value!==b&&(a.value=b)}
        function I(a){var b=a.value,c=a.ownerDocument.createEvent('HTMLEvents'),d=a.ownerDocument.createEvent('HTMLEvents');a.dispatchEvent(K(a,'keydown'));a.dispatchEvent(K(a,'keypress'));a.dispatchEvent(K(a,'keyup'));d.initEvent('input',!0,!0);a.dispatchEvent(d);c.initEvent('change',!0,!0);a.dispatchEvent(c);a.blur();a.value!==b&&(a.value=b)}function C(a){if(!a||a&&'function'!==typeof a.click)return!1;a.click();return!0}
        function L(){var a=RegExp('((\\\\b|_|-)pin(\\\\b|_|-)|password|passwort|kennwort|passe|contraseña|senha|密码|adgangskode|hasło|wachtwoord)','i');return Array.prototype.slice.call(A("input[type='text']")).filter(function(b){return b.value&&a.test(b.value)},this)}function D(){L().forEach(function(a){H(a);a.click&&a.click();I(a)})}
        window.LOGIN_TITLES=[/^\\W*log\\W*[oi]n\\W*$/i,/log\\W*[oi]n (?:securely|now)/i,/^\\W*sign\\W*[oi]n\\W*$/i,'continue','submit','weiter','accès','вход','connexion','entrar','anmelden','accedi','valider','登录','लॉग इन करें','change password'];window.LOGIN_RED_HERRING_TITLES=['already have an account','sign in with'];window.REGISTER_TITLES='register;sign up;signup;join;create my account;регистрация;inscription;regístrate;cadastre-se;registrieren;registrazione;注册;साइन अप करें'.split(';');
        window.SEARCH_TITLES='search find поиск найти искать recherche suchen buscar suche ricerca procurar 検索'.split(' ');window.FORGOT_PASSWORD_TITLES='forgot geändert vergessen hilfe changeemail español'.split(' ');window.REMEMBER_ME_TITLES=['remember me','rememberme','keep me signed in'];window.BACK_TITLES=['back','назад'];
        function J(a){var b;if(b=k)a:{b=a;for(var c=a.ownerDocument,c=c?c.defaultView:{},d;b&&b!==document;){d=c.getComputedStyle?c.getComputedStyle(b,null):b.style;if(!d){b=!0;break a}if('none'===d.display||'hidden'==d.visibility){b=!1;break a}b=b.parentNode}b=b===document}return b?-1!=='email text password number tel url'.split(' ').indexOf(a.type||''):!1}
        function y(a){var b;if(void 0===a||null===a)return null;try{var c=Array.prototype.slice.call(A('input, select, button')),d=c.filter(function(b){return b.opid==a});if(0<d.length)b=d[0],1<d.length&&console.warn('More than one element found with opid '+a);else{var e=parseInt(a.split('__')[1],10);isNaN(e)||(b=c[e])}}catch(h){console.error('An unexpected error occurred: '+h)}finally{return b}};function A(a){var b=document,c=[];try{c=b.querySelectorAll(a)}catch(d){}return c}function B(a,b){if(b){var c=a.value;a.focus();a.value!==c&&(a.value=c)}else a.focus()};
        m(fillScript);
        return JSON.stringify({'success': true});
    }
};

var ExtensionPreprocessingJS = new BitwardenExtension;
