# Assets

This directory is the curated source layer for application assets. The original files under `参考资料/product_images/` remain unchanged as task evidence.

Before application seeding, add an explicit manifest that maps a stable asset identifier and filename to a dish record. File order or names such as `image_01...jpg` must not be treated as a hidden business mapping. Runtime uploads should be stored outside the application image through a storage adapter and should not be committed here.
