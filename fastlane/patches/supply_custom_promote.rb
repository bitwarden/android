# Patch Description:
# Fixes issue where Fastlane 'Supply' doesn't recognize previous builds
# when promoting to another track.
#
# Source: https://github.com/artsy/eigen/pull/10262
# Author: Brian Beckerle (@brainbicycle)
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
      UI.message("Total releases in track: #{releases.length}")
      if releases.length == 1
        UI.message("One release found: name[#{release.name}] status[#{release.status}] code[#{release.version_codes}]")
      end

      version_code = Supply.config[:version_code].to_s
      if !Supply.config[:skip_release_verification]
        if version_code != ""
          releases = releases.select do |release|
            release.version_codes.include?(version_code)
          end
        else
          releases = releases.select do |release|
            release.status == Supply.config[:release_status]
          end
        end

        if releases.size == 0
          if version_code != ""
            UI.user_error!("Cannot find release with version code '#{version_code}' to promote in track '#{Supply.config[:track]}'")
          else
            UI.user_error!("Track '#{Supply.config[:track]}' doesn't have any releases")
          end
        elsif releases.size > 1
          UI.user_error!("Track '#{Supply.config[:track]}' has more than one release - use :version_code to filter the release to promote")
        end
      else
        UI.message("Skipping release verification as per configuration.")
        if version_code == ""
          UI.user_error!("Must provide a version code when release verification is skipped.")
        end
        if Supply.config[:version_name].nil?
          UI.user_error!("To force promote a :version_code, it is mandatory to enter the :version_name")
        end
      end 
        # release = AndroidPublisher::TrackRelease.new(
        #   name: Supply.config[:version_name],
        #   version_codes: [version_code],
        #   status: Supply.config[:track_promote_release_status] || Supply::ReleaseStatus::COMPLETED
        # )

        # filter only releases that contain the target version code
        releases = releases.select do |release|
            release.version_codes == [version_code]
        end

        if releases.length < 1
          UI.user_error!("No releases match version code #{version_code}.")
        end

        if releases.length > 1
          UI.user_error!("Multiple releases match version code #{version_code}.")
        else
          release = releases.first
        end

        UI.message("Release info: name[#{release.name}] status[#{release.status}] code[#{release.version_codes}]")

        # releases = releases.select do |release|
        #     release.name == Supply.config[:version_name] &&
        #     release.version_codes == [version_code] &&
        #     release.status == 'completed'
        # end

      release = releases.first unless Supply.config[:skip_release_verification]
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

      UI.message("Promoting release with version: #{release.name} (#{release.version_codes.first} Track: #{Supply.config[:track_promote_to]})")

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
      UI.message("confirmed that update_track was reached: #{Supply.config[:track_promote_to]} #{release}")
    end
  end
  class Client
    def update_track(track_name, track_object)
      UI.message("Using custom `update_track` method.")
      ensure_active_edit!

      UI.message("name:#{track_name} object:#{track_object} package:#{current_package_name}")
      call_google_api do
        client.update_edit_track(
          current_package_name,
          current_edit.id,
          track_name,
          track_object
        )
      end
    end
  end
end
