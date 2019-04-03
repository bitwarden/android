const gulp = require('gulp');
const gulpSass = require('gulp-sass');
const del = require('del');
const fs = require('fs');

gulpSass.compiler = require('node-sass');

const paths = {
    css: './src/App/Css',
};

const sassFiles = [
    './src/App/Sass/android.scss',
    './src/App/Sass/ios.scss',
    './src/App/Sass/dark.scss',
    './src/App/Sass/styles.scss',
];

function sass() {
    return gulp.src(sassFiles)
        .pipe(gulpSass().on('error', gulpSass.logError))
        .pipe(gulp.dest(paths.css));
}

function fixSass(cb) {
    fs.readdir(paths.css, (err, cssFiles) => {
        cssFiles.forEach((cssFile) => {
            const file = paths.css + '/' + cssFile;
            let fileContents = fs.readFileSync(file, 'utf8');
            fileContents = fileContents.replace(/__/g, '^');
            fs.writeFileSync(file, fileContents, 'utf8');
        });
        cb();
    });
}

function cleanCss() {
    return del(paths.css);
}

exports.sass = gulp.series(cleanCss, sass, fixSass);
