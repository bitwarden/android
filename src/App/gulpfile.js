const gulp = require('gulp');
const gulpLess = require('gulp-less');
const del = require('del');

function less() {
    return gulp.src(['./Less/android.less', './Less/ios.less', './Less/dark.less', './Less/styles.less'])
        .pipe(gulpLess())
        .pipe(gulp.dest('./Css'));
}

function cleanCss() {
    return del('./Css');
}

exports.cleanCss = cleanCss;
exports.less = gulp.series(cleanCss, less);
