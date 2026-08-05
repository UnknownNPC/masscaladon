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
  val accountId = AccountIdApi
  val accounts = AccountsApi
  val announcements = AnnouncementsApi
  val annualReports = AnnualReportsApi
  val apps = AppsApi
  val asyncRefreshes = AsyncRefreshesApi
  val blocks = BlocksApi
  val bookmarks = BookmarksApi
  val collections = CollectionsApi
  val conversations = ConversationsApi
  val customEmojis = CustomEmojisApi
  val directory = DirectoryApi
  val domainBlocks = DomainBlocksApi
  val emails = EmailsApi
  val endorsements = EndorsementsApi
  val favourites = FavouritesApi
  val featuredTags = FeaturedTagsApi
  val filters = FiltersApi
  val followRequests = FollowRequestsApi
  val followedTags = FollowedTagsApi
  val health = HealthApi
  val instance = InstanceApi
  val lists = ListsApi
  val markers = MarkersApi
  val media = MediaApi
  val mutes = MutesApi
  val notifications = NotificationsApi
  val oauth = OauthApi
  val oembed = OembedApi
  val polls = PollsApi
  val preferences = PreferencesApi
  val profile = ProfileApi
  val push = PushApi
  val reports = ReportsApi
  val scheduledStatuses = ScheduledStatusesApi
  val search = SearchApi
  val statuses = StatusesApi
  val streaming = StreamingApi
  val suggestions = SuggestionsApi
  val tags = TagsApi
  val timelines = TimelinesApi
  val trends = TrendsApi
  val wellKnown = WellKnownApi
