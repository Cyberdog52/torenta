import { createServer } from 'node:http';

const port = Number(process.env.CONCIERGE_MOCK_PORT ?? 10999);
const state = {
  ollamaCalls: [],
  tmdbRequests: [],
};

const movie = {
  adult: false,
  backdrop_path: null,
  genre_ids: [878],
  id: 101,
  original_language: 'en',
  original_title: 'Moonlit Journey',
  overview: 'An astronaut follows a mysterious signal home.',
  popularity: 75,
  poster_path: null,
  release_date: '2016-04-15',
  title: 'Moonlit Journey',
  video: false,
  vote_average: 8.2,
  vote_count: 900,
};

const series = {
  backdrop_path: null,
  first_air_date: '2018-09-20',
  genre_ids: [10765],
  id: 202,
  name: 'Bright Horizons',
  origin_country: ['US'],
  original_language: 'en',
  original_name: 'Bright Horizons',
  overview: 'A hopeful crew builds a new life among the stars.',
  popularity: 85,
  poster_path: null,
  vote_average: 8.8,
  vote_count: 1200,
};

const intent = {
  mediaType: 'ANY',
  moods: ['hopeful'],
  similarTo: null,
  numericFilters: [
    { key: 'YEAR', operator: 'EQ', value: 0, evidence: '' },
    { key: 'VOTE_AVERAGE', operator: 'GTE', value: 7, evidence: 'rated at least 7' },
    { key: 'RUNTIME', operator: 'LTE', value: 130, evidence: 'under 130 minutes' },
  ],
  dateFilters: [
    {
      key: 'PRIMARY_RELEASE_DATE',
      operator: 'GTE',
      value: '2010-01-01',
      evidence: 'from 2010 to 2020',
    },
    {
      key: 'PRIMARY_RELEASE_DATE',
      operator: 'LTE',
      value: '2020-12-31',
      evidence: 'from 2010 to 2020',
    },
    {
      key: 'FIRST_AIR_DATE',
      operator: 'GTE',
      value: '2010-01-01',
      evidence: 'from 2010 to 2020',
    },
    {
      key: 'FIRST_AIR_DATE',
      operator: 'LTE',
      value: '2020-12-31',
      evidence: 'from 2010 to 2020',
    },
  ],
  textFilters: [{ key: 'ORIGINAL_LANGUAGE', value: 'en', evidence: 'English' }],
  booleanFilters: [],
  namedFilters: [
    {
      key: 'GENRE',
      names: ['Science Fiction'],
      polarity: 'INCLUDE',
      matching: 'ANY',
      evidence: 'science fiction',
    },
    {
      key: 'NETWORK',
      names: ['Imaginary Network'],
      polarity: 'INCLUDE',
      matching: 'ANY',
      evidence: 'Imaginary Network',
    },
  ],
  enumFilters: [],
};

const rankings = [
  {
    rank: 1,
    mediaType: 'SERIES',
    id: series.id,
    candidateKey: `SERIES:${series.id}`,
    score: 95,
    title: series.name,
    explanation: 'Its optimistic space-community story best matches your hopeful mood.',
  },
  {
    rank: 2,
    mediaType: 'MOVIE',
    id: movie.id,
    candidateKey: `MOVIE:${movie.id}`,
    score: 90,
    title: movie.title,
    explanation: 'A highly rated science-fiction journey within your requested years.',
  },
];

function json(response, status, value) {
  response.writeHead(status, { 'content-type': 'application/json' });
  response.end(JSON.stringify(value));
}

async function readJson(request) {
  const chunks = [];
  for await (const chunk of request) {
    chunks.push(chunk);
  }
  const content = Buffer.concat(chunks).toString('utf8');
  return content ? JSON.parse(content) : {};
}

function ollamaKind(body) {
  const text = JSON.stringify(body).toLowerCase();
  return text.includes('candidate') ||
    text.includes('moonlit journey') ||
    text.includes('bright horizons')
    ? 'ranking'
    : 'intent';
}

function ollamaContent(kind) {
  if (kind === 'intent') {
    return JSON.stringify(intent);
  }
  return JSON.stringify({
    rankings,
    results: rankings,
  });
}

function movieDetail() {
  return {
    ...movie,
    backdrop_path: null,
    budget: 50_000_000,
    genres: [{ id: 878, name: 'Science Fiction' }],
    homepage: '',
    imdb_id: 'tt0000101',
    production_companies: [],
    revenue: 120_000_000,
    runtime: 112,
    spoken_languages: [{ english_name: 'English', iso_639_1: 'en', name: 'English' }],
    status: 'Released',
    tagline: 'Home is closer than it seems.',
  };
}

function seriesDetail() {
  return {
    ...series,
    created_by: [],
    episode_run_time: [48],
    genres: [{ id: 878, name: 'Science Fiction' }],
    homepage: '',
    in_production: false,
    languages: ['en'],
    last_air_date: '2020-05-01',
    last_episode_to_air: null,
    networks: [],
    next_episode_to_air: null,
    number_of_episodes: 16,
    number_of_seasons: 2,
    production_companies: [],
    seasons: [],
    status: 'Ended',
    type: 'Scripted',
  };
}

const server = createServer(async (request, response) => {
  const url = new URL(request.url ?? '/', `http://${request.headers.host}`);

  if (request.method === 'GET' && url.pathname === '/__state') {
    json(response, 200, state);
    return;
  }
  if (request.method === 'POST' && url.pathname === '/__reset') {
    state.ollamaCalls.length = 0;
    state.tmdbRequests.length = 0;
    json(response, 200, { ok: true });
    return;
  }
  if (
    request.method === 'GET' &&
    (url.pathname === '/api/tags' || url.pathname === '/api/version')
  ) {
    json(
      response,
      200,
      url.pathname.endsWith('tags')
        ? { models: [{ name: 'playwright-model', model: 'playwright-model' }] }
        : { version: '0.0.0-playwright' },
    );
    return;
  }
  if (
    request.method === 'POST' &&
    (url.pathname === '/api/chat' || url.pathname === '/api/generate')
  ) {
    const body = await readJson(request);
    const kind = ollamaKind(body);
    state.ollamaCalls.push({ kind, request: JSON.stringify(body) });
    const content = ollamaContent(kind);
    json(
      response,
      200,
      url.pathname.endsWith('chat')
        ? {
            model: 'playwright-model',
            created_at: new Date().toISOString(),
            message: { role: 'assistant', content },
            done: true,
            done_reason: 'stop',
            total_duration: 1,
            load_duration: 1,
            prompt_eval_count: 1,
            eval_count: 1,
          }
        : {
            model: 'playwright-model',
            created_at: new Date().toISOString(),
            response: content,
            done: true,
            done_reason: 'stop',
          },
    );
    return;
  }

  const isTmdb = /\/(discover|genre|movie|tv)\//.test(`${url.pathname}/`);
  if (request.method === 'GET' && isTmdb) {
    state.tmdbRequests.push({
      path: url.pathname,
      query: Object.fromEntries(url.searchParams),
    });
    if (/\/genre\/movie\/list$/.test(url.pathname)) {
      json(response, 200, { genres: [{ id: 878, name: 'Science Fiction' }] });
      return;
    }
    if (/\/genre\/tv\/list$/.test(url.pathname)) {
      json(response, 200, { genres: [{ id: 10765, name: 'Sci-Fi & Fantasy' }] });
      return;
    }
    if (/\/discover\/movie$/.test(url.pathname)) {
      json(response, 200, { page: 1, results: [movie], total_pages: 1, total_results: 1 });
      return;
    }
    if (/\/discover\/tv$/.test(url.pathname)) {
      json(response, 200, { page: 1, results: [series], total_pages: 1, total_results: 1 });
      return;
    }
    if (new RegExp(`/movie/${movie.id}$`).test(url.pathname)) {
      json(response, 200, movieDetail());
      return;
    }
    if (new RegExp(`/tv/${series.id}$`).test(url.pathname)) {
      json(response, 200, seriesDetail());
      return;
    }
  }

  json(response, 404, { error: `No fixture for ${request.method} ${url.pathname}` });
});

server.listen(port, '127.0.0.1', () => {
  console.log(`Concierge protocol fixture listening on http://127.0.0.1:${port}`);
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
