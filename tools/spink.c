#define _GNU_SOURCE

#include <stdio.h>
#include <unistd.h>
#include <dlfcn.h>

typedef void *(*malloc_fn_t)(size_t);
typedef void (*free_fn_t)(void *);

typedef void *(*realloc_fn_t)(void *, size_t);
typedef void *(*calloc_fn_t)(size_t, size_t);


static malloc_fn_t _orig_malloc;
static free_fn_t _orig_free;
static realloc_fn_t _orig_realloc;
static calloc_fn_t _orig_calloc;

static void __init_intercept_calloc() {
	_orig_calloc = dlsym(RTLD_NEXT, "calloc");
}

static void __init_intercept()
{
//	_orig_calloc = dlsym(RTLD_NEXT, "calloc");
	_orig_malloc = dlsym(RTLD_NEXT, "malloc");
	_orig_free = dlsym(RTLD_NEXT, "free");
	_orig_realloc = dlsym(RTLD_NEXT, "realloc");
}

void *malloc(size_t s)
{
	void *ptr;

	if (!_orig_malloc) __init_intercept();

	ptr = _orig_malloc(s);
	fprintf(stderr, "XXX: malloc(%lx) = %p\n", s, ptr);

	return ptr;
}

void free(void *p)
{
	if (!_orig_free) __init_intercept();

	fprintf(stderr, "XXX: free(%p)\n", p);
	_orig_free(p);
}

void *realloc(void *p, size_t size)
{
	void *ptr;
	if (!_orig_realloc) __init_intercept();

	ptr = _orig_realloc(p, size);
	fprintf(stderr, "XXX: realloc(%p, %lx) = %p\n", p, size, ptr);

	return ptr;
}

void *calloc(size_t num, size_t size)
{
	void *ptr;
	fprintf(stderr, "before\n");
	if (!_orig_calloc) __init_intercept_calloc();
	fprintf(stderr, "after\n");
	ptr = _orig_calloc(num, size);
	fprintf(stderr, "after c\n");
	fprintf(stderr, "XXX: calloc(%lx, %lx) = %p\n", num, size, ptr);

	return ptr;
}

