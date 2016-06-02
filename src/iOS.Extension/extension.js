var BitwardenExtension = function () { };

BitwardenExtension.prototype = {
    run: function (arguments) {
        console.log('Run');
        console.log(arguments);

        var args = {
            'url_string': document.URL,
            pageDetails: this.collect(document)
        };

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
        document.elementsByOPID={};
        function n(d,e){function f(a,b){var c=a[b];if('string'==typeof c)return c;c=a.getAttribute(b);return'string'==typeof c?c:null}function h(a,b){if(-1===['text','password'].indexOf(b.type.toLowerCase())||!(l.test(a.value)||l.test(a.htmlID)||l.test(a.htmlName)||l.test(a.placeholder)||l.test(a['label-tag'])||l.test(a['label-data'])||l.test(a['label-aria'])))return!1;if(!a.visible)return!0;if('password'==b.type.toLowerCase())return!1;var c=b.type,d=b.value;b.focus();b.value!==d&&(b.value=d);return c!==
        b.type}function r(a){switch(m(a.type)){case 'checkbox':return a.checked?'✓':'';case 'hidden':a=a.value;if(!a||'number'!=typeof a.length)return'';254<a.length&&(a=a.substr(0,254)+'...SNIPPED');return a;default:return a.value}}function v(a){return a.options?(a=Array.prototype.slice.call(a.options).map(function(a){var c=a.text,c=c?m(c).replace(/\\s/mg,'').replace(/[~`!@$%^&*()\\-_+=:;'\"\\[\\]|\\\\,<.>\\?]/mg,''):null;return[c?c:null,a.value]}),{options:a}):null}function F(a){var b;for(a=a.parentElement||a.parentNode;a&&
        'td'!=m(a.tagName);)a=a.parentElement||a.parentNode;if(!a||void 0===a)return null;b=a.parentElement||a.parentNode;if('tr'!=b.tagName.toLowerCase())return null;b=b.previousElementSibling;if(!b||'tr'!=(b.tagName+'').toLowerCase()||b.cells&&a.cellIndex>=b.cells.length)return null;a=s(b.cells[a.cellIndex]);return a=u(a)}function A(a){var b=d.documentElement,c=a.getBoundingClientRect(),e=b.getBoundingClientRect(),f=c.left-b.clientLeft,b=c.top-b.clientTop;return a.offsetParent?0>f||f>e.width||0>b||b>e.height?
        w(a):(e=a.ownerDocument.elementFromPoint(f+3,b+3))?'label'===m(e.tagName)?e===B(a):e.tagName===a.tagName:!1:!1}function w(a){for(var b;a!==d&&a;a=a.parentNode){b=t.getComputedStyle?t.getComputedStyle(a,null):a.style;if(!b)return!0;if('none'===b.display||'hidden'==b.visibility)return!1}return a===d}function B(a){var b=[];a.id&&(b=b.concat(Array.prototype.slice.call(x(d,'label[for='+JSON.stringify(a.id)+']'))));a.name&&(b=b.concat(Array.prototype.slice.call(x(d,'label[for='+JSON.stringify(a.name)+']'))));
        if(0<b.length)return b.map(function(a){return s(a)}).join('');for(;a&&a!=d;a=a.parentNode)if('label'===m(a.tagName))return s(a);return null}function g(a,b,c,d){void 0!==d&&d===c||null===c||void 0===c||(a[b]=c)}function m(a){return'string'===typeof a?a.toLowerCase():(''+a).toLowerCase()}function x(a,b){var c=[];try{c=a.querySelectorAll(b)}catch(d){}return c}var t=d.defaultView?d.defaultView:window,p,l=RegExp('((\\\\b|_|-)pin(\\\\b|_|-)|password|passwort|kennwort|passe|contraseña|senha|密码|adgangskode|hasło|wachtwoord)',
        'i');p=Array.prototype.slice.call(x(d,'form')).map(function(a,b){var c={},d='__form__'+b;a.opid=d;c.opid=d;g(c,'htmlName',f(a,'name'));g(c,'htmlID',f(a,'id'));g(c,'htmlAction',y(f(a,'action')));g(c,'htmlMethod',f(a,'method'));return c});var q=Array.prototype.slice.call(z(d)).map(function(a,b){var c={},e='__'+b,k=-1==a.maxLength?999:a.maxLength;if(!k||'number'===typeof k&&isNaN(k))k=999;d.elementsByOPID[e]=a;a.opid=e;c.opid=e;c.elementNumber=b;g(c,'maxLength',Math.min(k,999),999);c.visible=w(a);c.viewable=
        A(a);g(c,'htmlID',f(a,'id'));g(c,'htmlName',f(a,'name'));g(c,'htmlClass',f(a,'class'));g(c,'tabindex',f(a,'tabindex'));if('hidden'!=m(a.type)){g(c,'label-tag',B(a));g(c,'label-data',f(a,'data-label'));g(c,'label-aria',f(a,'aria-label'));g(c,'label-top',F(a));e=[];for(k=a;k&&k.nextSibling;){k=k.nextSibling;if(C(k))break;D(e,k)}g(c,'label-right',e.join(''));e=[];E(a,e);e=e.reverse().join('');g(c,'label-left',e);g(c,'placeholder',f(a,'placeholder'))}g(c,'rel',f(a,'rel'));g(c,'type',m(f(a,'type')));g(c,
        'value',r(a));g(c,'checked',a.checked,!1);g(c,'autoCompleteType',a.getAttribute('x-autocompletetype')||a.getAttribute('autocompletetype')||a.getAttribute('autocomplete'),'off');g(c,'disabled',a.disabled);g(c,'readonly',a.a||a.readOnly);g(c,'selectInfo',v(a));g(c,'aria-hidden','true'==a.getAttribute('aria-hidden'),!1);g(c,'aria-disabled','true'==a.getAttribute('aria-disabled'),!1);g(c,'aria-haspopup','true'==a.getAttribute('aria-haspopup'),!1);g(c,'data-unmasked',a.dataset.unmasked);g(c,'data-stripe',
        f(a,'data-stripe'));g(c,'onepasswordFieldType',a.dataset.onepasswordFieldType||a.type);g(c,'onepasswordDesignation',a.dataset.onepasswordDesignation);g(c,'onepasswordSignInUrl',a.dataset.onepasswordSignInUrl);g(c,'onepasswordSectionTitle',a.dataset.onepasswordSectionTitle);g(c,'onepasswordSectionFieldKind',a.dataset.onepasswordSectionFieldKind);g(c,'onepasswordSectionFieldTitle',a.dataset.onepasswordSectionFieldTitle);g(c,'onepasswordSectionFieldValue',a.dataset.onepasswordSectionFieldValue);a.form&&
        (c.form=f(a.form,'opid'));g(c,'fakeTested',h(c,a),!1);return c});q.filter(function(a){return a.fakeTested}).forEach(function(a){var b=d.elementsByOPID[a.opid];b.getBoundingClientRect();var c=b.value;!b||b&&'function'!==typeof b.click||b.click();b.focus();G(b,'keydown');G(b,'keyup');G(b,'keypress');b.value!==c&&(b.value=c);b.click&&b.click();a.postFakeTestVisible=w(b);a.postFakeTestViewable=A(b);a.postFakeTestType=b.type;a=b.value;var c=b.ownerDocument.createEvent('HTMLEvents'),e=b.ownerDocument.createEvent('HTMLEvents');
        G(b,'keydown');G(b,'keyup');G(b,'keypress');e.initEvent('input',!0,!0);b.dispatchEvent(e);c.initEvent('change',!0,!0);b.dispatchEvent(c);b.blur();b.value!==a&&(b.value=a)});p={documentUUID:e,title:d.title,url:t.location.href,documentUrl:d.location.href,tabUrl:t.location.href,forms:function(a){var b={};a.forEach(function(a){b[a.opid]=a});return b}(p),fields:q,collectedTimestamp:(new Date).getTime()};(q=document.querySelector('[data-onepassword-display-title]'))&&q.dataset[DISPLAY_TITLE_ATTRIBUE]&&
        (p.displayTitle=q.dataset.onepasswordTitle);return p};document.elementForOPID=H;function G(d,e){var f;f=d.ownerDocument.createEvent('KeyboardEvent');f.initKeyboardEvent?f.initKeyboardEvent(e,!0,!0):f.initKeyEvent&&f.initKeyEvent(e,!0,!0,null,!1,!1,!1,!1,0,0);d.dispatchEvent(f)}window.LOGIN_TITLES=[/^\\W*log\\W*[oi]n\\W*$/i,/log\\W*[oi]n (?:securely|now)/i,/^\\W*sign\\W*[oi]n\\W*$/i,'continue','submit','weiter','accès','вход','connexion','entrar','anmelden','accedi','valider','登录','लॉग इन करें'];window.LOGIN_RED_HERRING_TITLES=['already have an account','sign in with'];
        window.REGISTER_TITLES='register;sign up;signup;join;регистрация;inscription;regístrate;cadastre-se;registrieren;registrazione;注册;साइन अप करें'.split(';');window.SEARCH_TITLES='search find поиск найти искать recherche suchen buscar suche ricerca procurar 検索'.split(' ');window.FORGOT_PASSWORD_TITLES='forgot geändert vergessen hilfe changeemail español'.split(' ');window.REMEMBER_ME_TITLES=['remember me','rememberme','keep me signed in'];window.BACK_TITLES=['back','назад'];
        function s(d){return d.textContent||d.innerText}function u(d){var e=null;d&&(e=d.replace(/^\\s+|\\s+$|\\r?\\n.*$/mg,''),e=0<e.length?e:null);return e}function D(d,e){var f;f='';3===e.nodeType?f=e.nodeValue:1===e.nodeType&&(f=s(e));(f=u(f))&&d.push(f)}function C(d){var e;d&&void 0!==d?(e='select option input form textarea button table iframe body head script'.split(' '),d?(d=d?(d.tagName||'').toLowerCase():'',e=e.constructor==Array?0<=e.indexOf(d):d===e):e=!1):e=!0;return e}
        function E(d,e,f){var h;for(f||(f=0);d&&d.previousSibling;){d=d.previousSibling;if(C(d))return;D(e,d)}if(d&&0===e.length){for(h=null;!h;){d=d.parentElement||d.parentNode;if(!d)return;for(h=d.previousSibling;h&&!C(h)&&h.lastChild;)h=h.lastChild}C(h)||(D(e,h),0===e.length&&E(h,e,f+1))}}
        function H(d){var e;if(void 0===d||null===d)return null;try{var f=Array.prototype.slice.call(z(document)),h=f.filter(function(e){return e.opid==d});if(0<h.length)e=h[0],1<h.length&&console.warn('More than one element found with opid '+d);else{var r=parseInt(d.split('__')[1],10);isNaN(r)||(e=f[r])}}catch(v){console.error('An unexpected error occurred: '+v)}finally{return e}};var I=/^[\\/\\?]/;function y(d){if(!d)return null;if(0==d.indexOf('http'))return d;var e=window.location.protocol+'//'+window.location.hostname;window.location.port&&''!=window.location.port&&(e+=':'+window.location.port);d.match(I)||(d='/'+d);return e+d}function z(d){var e=[];try{e=d.querySelectorAll('input, select, button')}catch(f){}return e};
        return JSON.stringify(n(document, 'oneshotUUID'));
    },
    fill: function(document, fillScript, undefined) {
        var f=!0,h=!0;
        function l(a){var b=null;return a?0===a.indexOf('https://')&&'http:'===document.location.protocol&&(b=document.querySelectorAll('input[type=password]'),0<b.length&&(confirmResult=confirm('Warning: This is an unsecured HTTP page, and any information you submit can potentially be seen and changed by others. This Login was originally saved on a secure (HTTPS) page.\\n\\nDo you still wish to fill this login?'),0==confirmResult))?!0:!1:!1}
        function k(a){var b,c=[],d=a.properties,e=1,g;d&&d.delay_between_operations&&(e=d.delay_between_operations);if(!l(a.savedURL)){g=function(a,b){var d=a[0];void 0===d?b():('delay'===d.operation||'delay'===d[0]?e=d.parameters?d.parameters[0]:d[1]:c.push(m(d)),setTimeout(function(){g(a.slice(1),b)},e))};if(b=a.options)b.hasOwnProperty('animate')&&(h=b.animate),b.hasOwnProperty('markFilling')&&(f=b.markFilling);a.itemType&&'fillPassword'===a.itemType&&(f=!1);a.hasOwnProperty('script')&&(b=a.script,g(b,
        function(){c=Array.prototype.concat.apply(c,void 0);a.hasOwnProperty('autosubmit')&&'function'==typeof autosubmit&&(a.itemType&&'fillLogin'!==a.itemType||setTimeout(function(){autosubmit(a.autosubmit,d.allow_clicky_autosubmit)},AUTOSUBMIT_DELAY));'object'==typeof protectedGlobalPage&&protectedGlobalPage.a('fillItemResults',{documentUUID:documentUUID,fillContextIdentifier:a.fillContextIdentifier,usedOpids:c},function(){fillingItemType=null})}))}}
        var v={fill_by_opid:n,fill_by_query:p,click_on_opid:q,click_on_query:r,touch_all_fields:s,simple_set_value_by_query:t,focus_by_opid:u,delay:null};function m(a){var b;if(a.hasOwnProperty('operation')&&a.hasOwnProperty('parameters'))b=a.operation,a=a.parameters;else if('[object Array]'===Object.prototype.toString.call(a))b=a[0],a=a.splice(1);else return null;return v.hasOwnProperty(b)?v[b].apply(this,a):null}function n(a,b){var c;return(c=w(a))?(x(c,b),c.opid):null}
        function p(a,b){var c;c=y(a);return Array.prototype.map.call(Array.prototype.slice.call(c),function(a){x(a,b);return a.opid},this)}function t(a,b){var c,d=[];c=y(a);Array.prototype.forEach.call(Array.prototype.slice.call(c),function(a){void 0!==a.value&&(a.value=b,d.push(a.opid))});return d}function u(a){if(a=w(a))'function'===typeof a.click&&a.click(),'function'===typeof a.focus&&a.focus();return null}function q(a){return(a=w(a))?z(a)?a.opid:null:null}
        function r(a){a=y(a);return Array.prototype.map.call(Array.prototype.slice.call(a),function(a){z(a);'function'===typeof a.click&&a.click();'function'===typeof a.focus&&a.focus();return a.opid},this)}function s(){A()};var B={'true':!0,y:!0,1:!0,yes:!0,'✓':!0},C=200;function x(a,b){var c;if(a&&null!==b&&void 0!==b)switch(f&&a.form&&!a.form.opfilled&&(a.form.opfilled=!0),a.type?a.type.toLowerCase():null){case 'checkbox':c=b&&1<=b.length&&B.hasOwnProperty(b.toLowerCase())&&!0===B[b.toLowerCase()];a.checked===c||D(a,function(a){a.checked=c});break;case 'radio':!0===B[b.toLowerCase()]&&a.click();break;default:a.value==b||D(a,function(a){a.value=b})}}
        function D(a,b){E(a);b(a);F(a);G(a)&&(a.className+=' com-agilebits-onepassword-extension-animated-fill',setTimeout(function(){a&&a.className&&(a.className=a.className.replace(/(\\s)?com-agilebits-onepassword-extension-animated-fill/,''))},C))};document.elementForOPID=w;function H(a,b){var c;c=a.ownerDocument.createEvent('KeyboardEvent');c.initKeyboardEvent?c.initKeyboardEvent(b,!0,!0):c.initKeyEvent&&c.initKeyEvent(b,!0,!0,null,!1,!1,!1,!1,0,0);a.dispatchEvent(c)}function E(a){var b=a.value;z(a);a.focus();H(a,'keydown');H(a,'keyup');H(a,'keypress');a.value!==b&&(a.value=b)}
        function F(a){var b=a.value,c=a.ownerDocument.createEvent('HTMLEvents'),d=a.ownerDocument.createEvent('HTMLEvents');H(a,'keydown');H(a,'keyup');H(a,'keypress');d.initEvent('input',!0,!0);a.dispatchEvent(d);c.initEvent('change',!0,!0);a.dispatchEvent(c);a.blur();a.value!==b&&(a.value=b)}function z(a){if(!a||a&&'function'!==typeof a.click)return!1;a.click();return!0}
        function I(){var a=RegExp('((\\\\b|_|-)pin(\\\\b|_|-)|password|passwort|kennwort|passe|contraseña|senha|密码|adgangskode|hasło|wachtwoord)','i');return Array.prototype.slice.call(y("input[type='text']")).filter(function(b){return b.value&&a.test(b.value)},this)}function A(){I().forEach(function(a){E(a);a.click&&a.click();F(a)})}
        window.LOGIN_TITLES=[/^\\W*log\\W*[oi]n\\W*$/i,/log\\W*[oi]n (?:securely|now)/i,/^\\W*sign\\W*[oi]n\\W*$/i,'continue','submit','weiter','accès','вход','connexion','entrar','anmelden','accedi','valider','登录','लॉग इन करें'];window.LOGIN_RED_HERRING_TITLES=['already have an account','sign in with'];window.REGISTER_TITLES='register;sign up;signup;join;регистрация;inscription;regístrate;cadastre-se;registrieren;registrazione;注册;साइन अप करें'.split(';');window.SEARCH_TITLES='search find поиск найти искать recherche suchen buscar suche ricerca procurar 検索'.split(' ');
        window.FORGOT_PASSWORD_TITLES='forgot geändert vergessen hilfe changeemail español'.split(' ');window.REMEMBER_ME_TITLES=['remember me','rememberme','keep me signed in'];window.BACK_TITLES=['back','назад'];
        function G(a){var b;if(b=h)a:{b=a;for(var c=a.ownerDocument,c=c?c.defaultView:{},d;b&&b!==document;){d=c.getComputedStyle?c.getComputedStyle(b,null):b.style;if(!d){b=!0;break a}if('none'===d.display||'hidden'==d.visibility){b=!1;break a}b=b.parentNode}b=b===document}return b?-1!=='email text password number tel url'.split(' ').indexOf(a.type||''):!1}
        function w(a){var b;if(void 0===a||null===a)return null;try{var c=Array.prototype.slice.call(y('input, select, button')),d=c.filter(function(b){return b.opid==a});if(0<d.length)b=d[0],1<d.length&&console.warn('More than one element found with opid '+a);else{var e=parseInt(a.split('__')[1],10);isNaN(e)||(b=c[e])}}catch(g){console.error('An unexpected error occurred: '+g)}finally{return b}};function y(a){var b=document,c=[];try{c=b.querySelectorAll(a)}catch(d){}return c};
        k(fillScript);
        return JSON.stringify({'success': true});
    }
};

var ExtensionPreprocessingJS = new BitwardenExtension;
