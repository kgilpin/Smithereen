## ActivityPub federation in Smithereen
Here are the non-standard additions to ActivityPub that Smithereen uses in order to federate between servers.

The `sm:` JSON-LD namespace is short for `http://smithereen.software/ns#`; in relevant objects, any custom fields within that namespace are aliased in `@context` and thus appear throughout the object without any namespace.
### Wall posts
People can post on other people's walls. Wall posts are part of the `sm:wall` collection, as per [FEP-400e](https://git.activitypub.dev/ActivityPubDev/Fediverse-Enhancement-Proposals/src/commit/f94077e1514928c2d2ae79d86a5953c93874b73d/feps/fep-400e.md). Addressing **must** include `as:Public` and the wall owner.

Example object:
```json
{
  "@context": [
    "https://www.w3.org/ns/activitystreams",
    {
      "sensitive": "as:sensitive"
    }
  ],
  "type": "Note",
  "id": "http://smithereen.local:8080/posts/110",
  "url": "http://smithereen.local:8080/posts/110",
  "to": [],
  "cc": [
    "https://www.w3.org/ns/activitystreams#Public",
    "http://smithereen.local:8080/users/4"
  ],
  "target": {
    "id": "http://smithereen.local:8080/users/4/wall",
    "attributedTo": "http://smithereen.local:8080/users/4",
    "type": "Collection"
  },
  "replies": {
    "id": "http://smithereen.local:8080/posts/110/replies",
    "type": "Collection",
    "first": {
      "next": "http://smithereen.local:8080/posts/110/replies?page=1",
      "partOf": "http://smithereen.local:8080/posts/110/replies",
      "type": "CollectionPage"
    }
  },
  "attributedTo": "http://smithereen.local:8080/users/1",
  "published": "2020-05-21T19:05:00Z",
  "sensitive": false,
  "content": "<p>Test.<\/p>"
}
```
After someone posts on a wall, the wall owner sends `Add{Note}` (with a link to the new post) to their followers to signal that a new post was added to the `sm:wall` collection.
#### A note about comments
Since this is modelled after VK, comments aren't supposed to appear in newsfeeds by themselves; they only exist in the context of a top-level post. Thus, comments aren't addressed to anyone's followers. They're addressed to `as:Public`, the top-level post author, the parent comment author, and mentioned users, if any.
### Friends and Followers
Any bilateral followings are considered friends. Even though all followers are accepted automatically, `Accept{Follow}` is expected from remote servers. Actors from servers running software that allows manually reviewing and accepting followers, e.g. Mastodon, are supported.
### Friend requests
Friend requests are sent as `Offer{Follow}` activities, where the inner `Follow` activity is as if the friend request recepient is following its sender. Semantically, this is "I'm asking you to follow me back". In order for Smithereen to allow sending a friend request to a remote actor, that actor must have `sm:supportsFriendRequests` field set to `true`. A friend request may have a text message attached to it in `content` of the `Offer`.

Accepting a friend request is done by following the sender back, that is, simply sending a `Follow`.

Rejecting a friend request is done by sending a `Reject{Offer{Follow}}` activity.
### Additional profile fields
There are separate fields for first and last names, birth date, and gender, all based on schema.org. Those are `firstName`, `lastName`, `birthDate` and `gender`. `firstName` and `lastName` are respectively aliases to `givenName` and `familyName`. Birth date is in the format `YYYY-MM-DD`. Gender can either be `sc:Male` or `sc:Female`.
### Non-square profile pictures
Smithereen uses non-square profile pictures on the profile page. In order to retain compatibility with everything else, `icon` in the actor still points to a square picture. It's extended with the `image` field that contains the full rectangular one, and `sm:cropRegion` with the coordinates of the square version within the rectangular one. The coordinates are in the order `[x1, y1, x2, y2]`, where (x1, y1) are the top-left corner of the square, and (x2, y2) are the bottom-right. The top-left corner of the rectangle is (0, 0), and the bottom-right one is (1, 1).

Example:
```json lines
...
  "icon": {
    "type": "Image",
    "url": "http://smithereen.local:8080/s/uploads/avatars/a502dd6ba23da7a899526368b9b58896_xl.jpg",
    "width": 400,
    "height": 400,
    "image": {
      "type": "Image",
      "url": "http://smithereen.local:8080/s/uploads/avatars/a502dd6ba23da7a899526368b9b58896_rxl.jpg",
      "width": 400,
      "height": 565
    },
    "cropRegion": [
      0.0555555559694767,
      0.05295007675886154,
      0.745726466178894,
      0.5416036248207092
    ]
  },
...
```
### Groups
Groups are like users, except they can't follow anything. Groups have walls that work the same way as user walls. Both `Join` and `Follow` activities work for joining groups, as well as `Leave` and `Undo{Follow}` for leaving. Outgoing activities are `Follow` and `Undo{Follow}` in order to maximize the compatibility.

Groups have administrators that are listed in the `attributedTo` field:
```json lines
  "attributedTo": [
    {
      "type": "Person",
      "title": "Group admins can have user-visible titles",
      "id": "http://smithereen.local:8080/users/1"
    }
  ]
```
These links must point to a `Person` object and will be ignored otherwise.

Any actions of the group administrators are federated as if the group actor itself performed them.

A group has one of three access types, specified in `sm:accessType` field:

* `open`: all content is public. Anyone can join and/or participate (unless blocked, of course). Joining the group is **not** required to post in it or interact with its content. This also is the default if no `sm:accessType` field is present.
* `closed`: the profile and the member list are public, but the content is private and visible to members only. You become a member after either sending a join request that is then manually reviewed and accepted by the group staff (the usual `Join`/`Accept{Join}` flow) or being invited by an existing group member (see below for the group invitations).
* `private`: nothing is public, including the profile. The only way to join is to be invited. Also, only group staff can send invitations.

#### Access control in non-public groups
To control access to the content in closed and private groups, Smithereen employs two mechanisms: signed GET requests and so-called "actor tokens".

To fetch an object from **the server that hosts the group** (including the `Group` actor itself for private groups), you need to sign your GET request with an HTTP signature using the key of **any** actor from a server that has members in the group. Smithereen itself always uses the service actor for this purpose, `/activitypub/serviceActor`. The rationale for this is that most ActivityPub servers only fetch and store a single copy of each object for all users to whom it may concern, and are responsible themselves for enforcing the visibility rules, if any, either way.

The process of fetching an object from **other** server involves an **actor token**. An actor token is a cryptographically signed temporary proof of membership in a group. Since it would be impractical to provide a revocation mechanism, an actor token has a limited validity time in order to account for cases when someone has left a group or was removed from it. It is a JSON object with the following fields:

* `issuer`: ID of the actor that generated this token
* `actor`: ID of the actor that the token is issued to (and must be presented with a valid HTTP signature of)
* `issuedAt`: timestamp when the token was generated, ISO-8601 instant (same format as ActivityPub timestamps)
* `validUntil`: timestamp when the token expires, ISO-8601 instant
* `signatures`: array of signature objects, currently with only one possible, and required, element defined:
  * `algorithm`: must be the string `rsa-sha256`
  * `keyId`: key ID, same as in HTTP signatures (e.g. `https://example.com/groups/1#main-key`)
  * `signature`: the RSA-SHA256 signature itself encoded as base64, see below for details

To obtain an actor token, make a signed GET request to the endpoint specified in `sm:actorToken` under `endpoints` in the actor object.

To use an actor token when fetching an object, pass it as `Authorization: ActivityPubActorToken {...}` HTTP header.

To generate a source string for signature:

1. Iterate over the keys in the actor token JSON object, skipping `signature`, and transform them into the format `key: value`. Add these strings to an array.
2. Sort the resulting array lexicographically.
3. Join the strings with newline character (`\n`, U+000A).
4. Convert the resulting string to a UTF-8 byte array.

To generate an actor token:

1. Verify that the requesting actor, as per HTTP signature, has access to the group (there are members with the same domain). If it does not, return a 403 error and stop.
2. Create a JSON object with the fields above (except `signature`). It is recommended that the validity period is 30 minutes, and it must not exceed 2 hours.
3. Generate a signature source string as above, sign it, and wrap it into an object with `signature`, `algorithm`, and `keyId` fields.
4. Add the object as a single element in the `signatures` array.

To verify an actor token:

1. Check that the HTTP signature is valid, and that `actor` in the token object matches the actor ID from `keyId` in the HTTP signature.
2. In the `signatures` array, find an object that has `algorithm` set to `rsa-sha256` to get the `signature` value. If there isn't any, return a 403 and stop.
3. Check the validity time: `issuedAt` must be in the past, `validUntil` must be in the future, and the difference between them must not exceed 2 hours. It is recommended to apply some margin to these checks to account for imprecisely set clocks. Smithereen uses 5 minutes.
4. Generate the signature source string as above and verify the signature.
5. Check that the object the requester is accessing is, in fact, part of a collection owned by `issuer`.

#### Events & tentative membership
An event is an extension of group. An event is identified as such by having an `Event` object in its `attachments`:
```json lines
{
  "type": "Group",
  "id": "https://friends.grishka.me/groups/70",
  "attachment": [
    {
      "type": "Event",
      "startTime": "2022-07-15T09:00:00Z"
    }
  ],
  "name": "Встреча с Гришкой в Макдональдсе",
  /* ... more fields ... */
}
```
The `Event` object must have `startTime` and may have `endTime`. There is currently no provisions for specifying the location of the event, but this is likely to change in the future.

Events can only have either `open` or `private` access type. It is possible to join an event tentatively ("I'm not sure I will attend"). Tentative membership adds the following:

* `sm:tentativeMembers` collection in the actor. Contains tentative members.
* `sm:TentativeJoin` activity type:
  * For non-members, joins them to the event tentatively and accepts invitation, if any.
  * For members, changes their decision by moving them from `followers` to `sm:tentativeMemebers`. (The reverse is done with regular `Join`/`Follow`.)
* `sm:tentativeMembership` element in `litepub:capabilities` to indicate support for this feature.

#### Invitations
It is possible to invite a friend (i.e. mutual follow) to a group by sending an `Invite{Group}` activity both to the group and to the user (there will be a privacy setting for this in the future).
* Anyone can invite to a `public` group.
* Only members can invite to a `closed` group.
* Only staff (listed under `attributedTo`) can invite to a `private` group.

It is important to send a copy of the `Invite` activity to the group itself so the group knows to expect that person to join. This is especially important for non-public groups because they would not accept that join otherwise.

Group staff can cancel a pending invitation by sending `Undo{Invite{Group}}` to the invitee from the group actor. To accept the invitation, the invitee simply joins the group (`Join`/`Follow`/`sm:TentativeJoin`). To decline the invitation, the invitee sends a `Reject{Invite{Group}}` to the group actor.

### The collection query endpoint
All Smithereen actors have `sm:collectionSimpleQuery` endpoint under `endpoints`. This is useful for when, for example, you've received a wall post, but you don't know whether the owner of the wall accepted that post. It supports these collections:

* `sm:wall`
* `sm:friends` (for user actors)
* `sm:groups` (for user actors)
* `sm:members` (for group actors)
* `sm:tentativeMembers` (for event actors)

The collection query endpoint accepts POST requests with form-data fields: `collection` for the collection ID (like `https://friends.grishka.me/users/1/wall`) and one or more `item` with the object IDs that you wish to test for presence in the collection. The result is a `sm:CollectionQueryResult` (which extends `CollectionPage`) containing only the object IDs that are actually present in the collection.

<details>
<summary>Request and response example</summary>

```
POST /users/1/collectionQuery HTTP/1.1
Content-Type: application/x-www-form-urlencoded; charset=utf-8
Host: smithereen.local:8080
Connection: close
User-Agent: Paw/3.3.6 (Macintosh; OS X/12.5.0) GCDHTTPRequest
Content-Length: 177

collection=http%3A%2F%2Fsmithereen.local%3A8080%2Fusers%2F1%2Ffriends&item=http%3A%2F%2Fsmithereen.local%3A8080%2Fusers%2F2&item=https%3A%2F%2Ffriends.grishka.me%2Fposts%2F85372
```

```json
{
  "type": "CollectionQueryResult",
  "items": [
    "http://smithereen.local:8080/users/2"
  ],
  "partOf": "http://smithereen.local:8080/users/1/friends",
  "@context": [
    "https://www.w3.org/ns/activitystreams",
    {
      "sm": "http://smithereen.software/ns#",
      "CollectionQueryResult": "sm:CollectionQueryResult"
    }
  ]
}
```
</details>

### Add and Remove activities for collections
For `sm:friends` and `sm:groups` collections for users, as well as `sm:members` and `sm:tentativeMembers` for groups and events, their owning actors send `Add` and `Remove` activities to their followers to help keep these lists in sync across servers. Smithereen also uses these activities to display entries like "John Smith added Jane Doe as a friend" in the news feed.