# Patch Description:
# Fixes issue where Fastlane 'Supply' doesn't recognize previous builds
# when promoting to another track.
#
# Enhanced to search by version name in addition to version code,
# and provide better error messages showing available versions.
#
# Original Source: https://github.com/artsy/eigen/pull/10262
# Author: Brian Beckerle (@brainbicycle)
# Enhanced by: Bitwarden Engineering
#

module Supply
  class Uploader
    alias_method :original_promote_track, :promote_track

    def promote_track
      if Supply.config[:skip_release_verification]
        custom_promote_track
      else
        original_promote_track
      end
    end

    def custom_promote_track
      UI.message("Using custom promotion logic")
      track_from = client.tracks(Supply.config[:track]).first
      unless track_from
        UI.user_error!("Cannot promote from track '#{Supply.config[:track]}' - track doesn't exist")
      end

      releases = track_from.releases

      # Log how many releases are available
      UI.message("Found #{releases.size} release(s) on track '#{Supply.config[:track]}'")

      version_code = Supply.config[:version_code].to_s
      version_name = Supply.config[:version_name].to_s

      if !Supply.config[:skip_release_verification]
        # Try to find the release by version code first
        if version_code != ""
          matching_releases = releases.select do |release|
            release.version_codes.include?(version_code)
          end

          if matching_releases.empty?
            # Provide helpful error with available versions (show up to 50 most recent)
            available_versions = releases.first(50).map do |r|
              "#{r.name} (#{r.version_codes.join(', ')})"
            end.join(", ")

            UI.user_error!(
              "Cannot find release with version code '#{version_code}' in track '#{Supply.config[:track]}'. " \
              "Searched through #{[50, releases.size].min} most recent releases. " \
              "Available versions: #{available_versions}"
            )
          end

          releases = matching_releases
        # If no version code but version name is provided, search by name
        elsif version_name != ""
          matching_releases = releases.select do |release|
            release.name == version_name
          end

          if matching_releases.empty?
            # Provide helpful error with available versions
            available_versions = releases.first(50).map do |r|
              "#{r.name} (#{r.version_codes.join(', ')})"
            end.join(", ")

            UI.user_error!(
              "Cannot find release with version name '#{version_name}' in track '#{Supply.config[:track]}'. " \
              "Searched through #{[50, releases.size].min} most recent releases. " \
              "Available versions: #{available_versions}"
            )
          end

          releases = matching_releases
        else
          # No version specified, filter by status
          releases = releases.select do |release|
            release.status == Supply.config[:release_status]
          end
        end

        if releases.size == 0
          if version_code != "" || version_name != ""
            UI.user_error!("Cannot find release matching version code '#{version_code}' or version name '#{version_name}' in track '#{Supply.config[:track]}'")
          else
            UI.user_error!("Track '#{Supply.config[:track]}' doesn't have any releases with status '#{Supply.config[:release_status]}'")
          end
        elsif releases.size > 1
          UI.user_error!(
            "Track '#{Supply.config[:track]}' has more than one release matching the criteria. " \
            "Found: #{releases.map { |r| "#{r.name} (#{r.version_codes.join(', ')})" }.join(', ')}. " \
            "Use :version_code to filter to a specific release."
          )
        end

        # Successfully found exactly one matching release
        release = releases.first
      else
        # Skipping verification - create synthetic release object
        UI.message("Skipping release verification as per configuration.")
        if version_code == ""
          UI.user_error!("Must provide a version code when release verification is skipped.")
        end
        if version_name == ""
          UI.user_error!("To force promote a :version_code, it is mandatory to enter the :version_name")
        end
        release = AndroidPublisher::TrackRelease.new(
          name: version_name,
          version_codes: [version_code],
          status: Supply.config[:track_promote_release_status] || Supply::ReleaseStatus::COMPLETED
        )
      end
      track_to = client.tracks(Supply.config[:track_promote_to]).first || AndroidPublisher::Track.new(
        track: Supply.config[:track_promote_to],
        releases: []
      )

      rollout = (Supply.config[:rollout] || 0).to_f
      if rollout > 0 && rollout < 1
        release.status = Supply::ReleaseStatus::IN_PROGRESS
        release.user_fraction = rollout
      else
        release.status = Supply.config[:track_promote_release_status]
        release.user_fraction = nil
      end

      UI.message("✅ Promoting release: #{release.name} (version code: #{release.version_codes.first}) from '#{Supply.config[:track]}' to '#{Supply.config[:track_promote_to]}'")

      if track_to
        # Its okay to set releases to an array containing the newest release
        # Google Play will keep previous releases there this release is a partial rollout
        track_to.releases = [release]
      else
        track_to = AndroidPublisher::Track.new(
          track: Supply.config[:track_promote_to],
          releases: [release]
        )
      end

      client.update_track(Supply.config[:track_promote_to], track_to)
      UI.message("✅ Successfully promoted to track: #{Supply.config[:track_promote_to]}")
    end
  end
end
