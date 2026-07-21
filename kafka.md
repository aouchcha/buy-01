````
User Service                    Topics                  Media Service
────────────────────────────────────────────────────────────────────
register / updateProfile
       │
       │  MediaUploadEvent
       │  { userId, fileName, content[] }
       └──────────> media.upload ──────────> MediaConsumer
                                                   │
                                          1. Tika (validation MIME)
                                          2. Upload → Cloudflare R2
                                                   │
                    media.upload.success <──────────┤ AcceptedUpload
                    media.upload.failed  <──────────┘ DeclinedUpload
                           │
              CheckEventConsumer
                    │
                    └─> userEntity.profilePictureUrl = event.MediaUrl()
                        Save MongoDB


```

```
Product Service                  Topics                    Media Service
──────────────────────────────────────────────────────────────────────────
POST /api/product/{id}/images
       │
       │  ProductImageUploadEvent
       │  { productId, userId, fileName, content[] }
       └──────────> media.product.upload ──────────> (à implémenter demain)
                                                            │
                   media.product.success <──────────────────┤ AcceptedUpload
                   media.product.failed  <──────────────────┘ DeclinedUpload
                           │
             ProductMediaEventConsumer
                    │
                    └─> product.imageUrls.add(event.MediaUrl())
                        Save MongoDB

```


jadid 

Frontend
  │
  ├─1─> POST /api/product ──────────> Product Service
  │                                        │ validate + save (sans images)
  │     <── { productId, name, ... } ──────┘
  │
  └─2─> POST /media/images?productId={id} ──> Media Service
                                                   │ validate MIME + size
                                                   │ upload → R2
                                                   │ Kafka: media.product.success
                                                   │   { productId, imageUrl }
             <── { imageUrl } ────────────────────┘
                                                   │
                                    Product Service Consumer
                                          │
                                          └─> product.imageUrls.add(imageUrl)
                                              Save MongoDB
