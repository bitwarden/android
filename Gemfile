source 'https://rubygems.org'

ruby File.read(".ruby-version").strip

gem 'fastlane', '2.229.1'
gem 'time', '0.4.2'

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)

# Since ruby 3.4.0 these are not included in the standard library
gem 'abbrev', '0.1.2'
gem 'logger', '1.7.0'
gem 'mutex_m', '0.3.0'
gem 'csv', '3.3.5'

# Since ruby 3.4.1 these are not included in the standard library
gem 'nkf', '0.2.0'

# Starting with Ruby 3.5.0, these are not included in the standard library
gem 'ostruct', '0.6.3'
