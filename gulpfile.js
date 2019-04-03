const gulp = require('gulp');
const gulpLess = require('gulp-less');
const del = require('del');
const fs = require('fs');

const paths = {
    css: './src/App/Css',
};

const lessFiles = [
    './src/App/Less/android.less',
    './src/App/Less/ios.less',
    './src/App/Less/dark.less',
    './src/App/Less/styles.less',
];

function less() {
    return gulp.src(lessFiles).pipe(gulpLess()).pipe(gulp.dest(paths.css));
}

function fixLess(cb) {
    fs.readdir(paths.css, (err, cssFiles) => {
        cssFiles.forEach((cssFile) => {
            const file = paths.css + '/' + cssFile;
            let fileContents = fs.readFileSync(file, 'utf8');
            fileContents = fileContents.replace(/ \^ /g, '^');
            fs.writeFileSync(file, fileContents, 'utf8');
        });
        cb();
    });
}

function cleanCss() {
    return del(paths.css);
}

exports.cleanCss = cleanCss;
exports.fixLess = fixLess;
exports.less = gulp.series(cleanCss, less, fixLess);
