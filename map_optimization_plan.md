# Map & Search Performance Improvement Plan

## 1. Search Logic Optimization (Priority: High)
*   **Current Issue**: Complex native SQL queries with hardcoded conditionals, difficult to maintain and potentially slow. Duplicate logic for different filter combinations.
*   **Action**:
    *   Refactor `SearchRepository` to strict separate `SearchRepositoryCustom` interface and `SearchRepositoryImpl` implementation.
    *   Use Dynamic JPQL or helper methods to build Native Queries dynamically (StringBuilder) instead of massive "one-size-fits-all" SQL blocks.
    *   Optimize joins and remove unnecessary subqueries where possible.
    *   **Caching**: Add Redis caching (`@Cacheable`) for `suggest` APIs which are read-heavy.

## 2. Map Performance Improvement (Priority: Medium-High)
*   **Current Issue**: Map resources (Kakao Maps SDK) may be loaded eagerly or unnecessarily. Rendering many markers on `MapView` can be slow.
*   **Action**:
    *   **Global**: Lazy load Kakao Maps SDK only when a map component is actually mounted and visible.
    *   **MapSection.vue (Detail Page)**:
        *   Implement `IntersectionObserver` to only render the map when the user scrolls to it.
        *   Consider showing a static map image (thumbnail) initially and loading the interactive map on interaction (click).
    *   **MapView.vue (Search Results)**:
        *   **Clustering**: Implement marker clustering for search results to improve rendering performance with many items.
        *   **Bound-Based Search**: Ensure the backend efficiently filters by viewport bounds (already implemented but needs verification/optimization in the new Custom Repository).

## 3. MainController Optimization
*   **Status**: `extractUserId` helper method has been added.
*   **Action**: Verify usage across all endpoints to ensure clean authentication context handling.
