source "https://rubygems.org"

ruby File.read(".ruby-version").strip

gem 'danger'
gem 'danger-shroud'
gem 'fastlane'
gem 'time'

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)
