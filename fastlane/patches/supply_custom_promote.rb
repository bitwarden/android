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
        release = AndroidPublisher::TrackRelease.new(
          version_codes: [version_code],
          status: Supply.config[:track_promote_release_status] || Supply::ReleaseStatus::COMPLETED
        )
      end

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
    end
  end
end
