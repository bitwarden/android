var BitwardenExtension = function () { };

BitwardenExtension.prototype = {
    run: function (arguments) {
        console.log("Run");
        console.log(arguments);

        var args = {
            content: document.body.innerHTML,
            uri: document.baseURI
        };
        arguments.completionFunction(args);
    },
    finalize: function (arguments) {
        console.log("Finalize");
        console.log(arguments);
    }
};

var ExtensionPreprocessingJS = new BitwardenExtension;
