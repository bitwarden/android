const gulp = require('gulp');
const gulpLess = require('gulp-less');
const del = require('del');

function less() {
    return gulp.src([
        './src/App/Less/android.less',
        './src/App/Less/ios.less',
        './src/App/Less/dark.less',
        './src/App/Less/styles.less'
    ]).pipe(gulpLess()).pipe(gulp.dest('./src/App/Css'));
}

function cleanCss() {
    return del('./src/App/Css');
}

exports.cleanCss = cleanCss;
exports.less = gulp.series(cleanCss, less);
