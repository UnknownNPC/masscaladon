package com.github.unknownnpc.masscaladon.client

import com.github.unknownnpc.masscaladon.generated.api.AccountIdApi
import com.github.unknownnpc.masscaladon.generated.api.AccountsApi
import com.github.unknownnpc.masscaladon.generated.api.AnnouncementsApi
import com.github.unknownnpc.masscaladon.generated.api.AnnualReportsApi
import com.github.unknownnpc.masscaladon.generated.api.AppsApi
import com.github.unknownnpc.masscaladon.generated.api.AsyncRefreshesApi
import com.github.unknownnpc.masscaladon.generated.api.BlocksApi
import com.github.unknownnpc.masscaladon.generated.api.BookmarksApi
import com.github.unknownnpc.masscaladon.generated.api.CollectionsApi
import com.github.unknownnpc.masscaladon.generated.api.ConversationsApi
import com.github.unknownnpc.masscaladon.generated.api.CustomEmojisApi
import com.github.unknownnpc.masscaladon.generated.api.DirectoryApi
import com.github.unknownnpc.masscaladon.generated.api.DomainBlocksApi
import com.github.unknownnpc.masscaladon.generated.api.EmailsApi
import com.github.unknownnpc.masscaladon.generated.api.EndorsementsApi
import com.github.unknownnpc.masscaladon.generated.api.FavouritesApi
import com.github.unknownnpc.masscaladon.generated.api.FeaturedTagsApi
import com.github.unknownnpc.masscaladon.generated.api.FiltersApi
import com.github.unknownnpc.masscaladon.generated.api.FollowRequestsApi
import com.github.unknownnpc.masscaladon.generated.api.FollowedTagsApi
import com.github.unknownnpc.masscaladon.generated.api.HealthApi
import com.github.unknownnpc.masscaladon.generated.api.InstanceApi
import com.github.unknownnpc.masscaladon.generated.api.ListsApi
import com.github.unknownnpc.masscaladon.generated.api.MarkersApi
import com.github.unknownnpc.masscaladon.generated.api.MediaApi
import com.github.unknownnpc.masscaladon.generated.api.MutesApi
import com.github.unknownnpc.masscaladon.generated.api.NotificationsApi
import com.github.unknownnpc.masscaladon.generated.api.OauthApi
import com.github.unknownnpc.masscaladon.generated.api.OembedApi
import com.github.unknownnpc.masscaladon.generated.api.PollsApi
import com.github.unknownnpc.masscaladon.generated.api.PreferencesApi
import com.github.unknownnpc.masscaladon.generated.api.ProfileApi
import com.github.unknownnpc.masscaladon.generated.api.PushApi
import com.github.unknownnpc.masscaladon.generated.api.ReportsApi
import com.github.unknownnpc.masscaladon.generated.api.ScheduledStatusesApi
import com.github.unknownnpc.masscaladon.generated.api.SearchApi
import com.github.unknownnpc.masscaladon.generated.api.StatusesApi
import com.github.unknownnpc.masscaladon.generated.api.StreamingApi
import com.github.unknownnpc.masscaladon.generated.api.SuggestionsApi
import com.github.unknownnpc.masscaladon.generated.api.TagsApi
import com.github.unknownnpc.masscaladon.generated.api.TimelinesApi
import com.github.unknownnpc.masscaladon.generated.api.TrendsApi
import com.github.unknownnpc.masscaladon.generated.api.WellKnownApi

/** Every request-creation object in one place, so they're easy to find. */
object Requests:
  lazy val accountId = AccountIdApi
  lazy val accounts = AccountsApi
  lazy val announcements = AnnouncementsApi
  lazy val annualReports = AnnualReportsApi
  lazy val apps = AppsApi
  lazy val asyncRefreshes = AsyncRefreshesApi
  lazy val blocks = BlocksApi
  lazy val bookmarks = BookmarksApi
  lazy val collections = CollectionsApi
  lazy val conversations = ConversationsApi
  lazy val customEmojis = CustomEmojisApi
  lazy val directory = DirectoryApi
  lazy val domainBlocks = DomainBlocksApi
  lazy val emails = EmailsApi
  lazy val endorsements = EndorsementsApi
  lazy val favourites = FavouritesApi
  lazy val featuredTags = FeaturedTagsApi
  lazy val filters = FiltersApi
  lazy val followRequests = FollowRequestsApi
  lazy val followedTags = FollowedTagsApi
  lazy val health = HealthApi
  lazy val instance = InstanceApi
  lazy val lists = ListsApi
  lazy val markers = MarkersApi
  lazy val media = MediaApi
  lazy val mutes = MutesApi
  lazy val notifications = NotificationsApi
  lazy val oauth = OauthApi
  lazy val oembed = OembedApi
  lazy val polls = PollsApi
  lazy val preferences = PreferencesApi
  lazy val profile = ProfileApi
  lazy val push = PushApi
  lazy val reports = ReportsApi
  lazy val scheduledStatuses = ScheduledStatusesApi
  lazy val search = SearchApi
  lazy val statuses = StatusesApi
  lazy val streaming = StreamingApi
  lazy val suggestions = SuggestionsApi
  lazy val tags = TagsApi
  lazy val timelines = TimelinesApi
  lazy val trends = TrendsApi
  lazy val wellKnown = WellKnownApi
