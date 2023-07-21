# ps1 - Podcast Server - Tapir, Akka-Stream and Slick demo

 This server allows to save podcasts (or RSS) content into a database, to update them keeping the new items and to assemble stored items back into the feeds.

It should work with podcast/RSS sources. The URLs of feeds are to be initially put into url field of podcast table.
Available endpoints:

    /docs             - automatically generated Swagger-doc
    /sql              - [text] automatically generated create SQL
    /podcast/all      - [JSON] API to get all podcasts or RSS
    /podcast/n        - [JSON] API to get podcast by id
    /podcast/n/items  - [JSON] API to get items by podcast by id
    /podcast/n/feed   - [XML] RSS/podcast feed by podcast id
    /podcast/n/update - [text stream] Update RSS/podcast by given id
    /podcast/update   - [text stream] Update all RSS/podcasts
    /channel/all      - [JSON] API to get all saved channels (header of feeds)
    /channel/n        - [JSON] API to get saved channels by id
    /item/all         - [JSON] API to get all saved items
    /item/n           - [JSON] API to get saved items by id
    /cat              = [HTML] Podcasts by channel category (automatic generated catalog)
