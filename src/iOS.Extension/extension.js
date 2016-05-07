var BitwardenExtension = function () { };

BitwardenExtension.prototype = {
    run: function (arguments) {
        var args = {
            content: document.body.innerHTML,
            uri: document.baseURI
        };
        arguments.completionFunction(args);
    },
    finalize: function (arguments) {
        alert('finalize');
    }
};

var ExtensionPreprocessingJS = new BitwardenExtension;
