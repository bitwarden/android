var BitwardenExtension = function () { };

BitwardenExtension.prototype = {
    run: function (arguments) {
        console.log("Run");
        console.log(arguments);

        var args = {
            baseUri: document.baseURI,
            url: document.URL,
            htmlContent: document.body.innerHTML
        };
        arguments.completionFunction(args);
    },
    finalize: function (arguments) {
        console.log("Finalize");
        console.log(arguments);
    }
};

var ExtensionPreprocessingJS = new BitwardenExtension;
