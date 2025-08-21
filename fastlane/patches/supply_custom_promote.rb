module Supply
  class Uploader
    # Alias the original promote_track method so we can override it
    alias_method :original_promote_track, :promote_track

    # Override promote_track to use custom logic if skip_release_verification is set
    def promote_track
      if Supply.config[:skip_release_verification]
        custom_promote_track
      else
        original_promote_track
      end
    end

    # Custom promotion logic for handling track promotion
    def custom_promote_track
      UI.message("Using custom promotion logic")

      # Fetch the source track to promote from
      track_from = client.tracks(Supply.config[:track]).first
      unless track_from
        UI.user_error!("Cannot promote from track '#{Supply.config[:track]}' - track doesn't exist")
      end

      # Gather all releases from the source track and log their version codes
      releases = track_from.releases
      version_name = Supply.config[:version_name]

      if version_name.nil? || version_name.empty?
        UI.message("No version name provided, skipping version code lookup.")
        return
      end

      all_tracks = client.tracks(nil) # nil fetches all tracks
      all_tracks.each do |track|
        track.releases.each do |release|
          if release.name == version_name
            UI.message("Track '#{track.track}' has release '#{version_name}' with version codes: #{release.version_codes.join(', ')}")
          end
        end
      end

      # Get the version code to promote
      version_code = Supply.config[:version_code].to_s

      # If release verification is not skipped, filter releases by version code or status
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

        # Error handling for missing or ambiguous releases
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
        # If skipping verification, require version code and version name
        UI.message("Skipping release verification as per configuration.")
        if version_code == ""
          UI.user_error!("Must provide a version code when release verification is skipped.")
        end
        if Supply.config[:version_name].nil?
          UI.user_error!("To force promote a :version_code, it is mandatory to enter the :version_name")
        end
        # Create a new release object for forced promotion
        release = AndroidPublisher::TrackRelease.new(
          name: Supply.config[:version_name],
          version_codes: [version_code],
          status: Supply.config[:track_promote_release_status] || Supply::ReleaseStatus::COMPLETED
        )
      end

      # Select the release to promote if verification is not skipped
      release = releases.first unless Supply.config[:skip_release_verification]

      # Fetch or create the destination track to promote to
      track_to = client.tracks(Supply.config[:track_promote_to]).first || AndroidPublisher::Track.new(
        track: Supply.config[:track_promote_to],
        releases: []
      )

      # Set rollout fraction and release status for partial rollouts
      rollout = (Supply.config[:rollout] || 0).to_f
      if rollout > 0 && rollout < 1
        release.status = Supply::ReleaseStatus::IN_PROGRESS
        release.user_fraction = rollout
      else
        release.status = Supply.config[:track_promote_release_status]
        release.user_fraction = nil
      end

      # Log the promotion action
      UI.message("Promoting release with version: #{release.name} (#{release.version_codes.first})")

      # Assign the release to the destination track
      if track_to
        track_to.releases = [release]
      else
        track_to = AndroidPublisher::Track.new(
          track: Supply.config[:track_promote_to],
          releases: [release]
        )
      end

      # Update the destination track with the new release
      client.update_track(Supply.config[:track_promote_to], track_to)
      UI.message("confirmed that update_track was reached: #{Supply.config[:track_promote_to]} #{release}")
    end
  end
end
