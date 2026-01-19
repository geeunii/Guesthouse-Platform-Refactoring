import test from 'node:test'
import assert from 'node:assert/strict'
import { setActivePinia, createPinia } from 'pinia'
import { useSearchStore } from '../src/stores/search.js'
import { normalizeKeyword, matchesKeyword } from '../src/utils/searchFilter.js'

test('normalizeKeyword trims and lowercases', () => {
  assert.equal(normalizeKeyword('  Seoul '), 'seoul')
  assert.equal(normalizeKeyword(null), '')
  assert.equal(normalizeKeyword(undefined), '')
})

test('matchesKeyword handles empty keyword', () => {
  const item = { title: 'Haeundae House', location: 'Busan Haeundae' }
  assert.equal(matchesKeyword(item, ''), true)
  assert.equal(matchesKeyword(item, '   '), true)
})

test('matchesKeyword checks title and location', () => {
  const item = { title: 'Haeundae House', location: 'Busan Haeundae' }
  assert.equal(matchesKeyword(item, 'busan'), true)
  assert.equal(matchesKeyword(item, 'house'), true)
  assert.equal(matchesKeyword(item, 'seoul'), false)
})

test('matchesKeyword is safe with missing fields', () => {
  const item = { title: null, location: undefined }
  assert.equal(matchesKeyword(item, 'busan'), false)
})

test('search store keyword setter', () => {
  setActivePinia(createPinia())
  const store = useSearchStore()
  assert.equal(store.keyword, '')
  store.setKeyword(' Busan ')
  assert.equal(store.keyword, ' Busan ')
  store.setKeyword(null)
  assert.equal(store.keyword, '')
})
