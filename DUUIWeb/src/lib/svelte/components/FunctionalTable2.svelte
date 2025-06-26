<script lang="ts">
	import Fa from 'svelte-fa';
	import { createEventDispatcher } from 'svelte';
	import Paginator from './Paginator.svelte';

	const dispatch = createEventDispatcher();

	export let title: string;
	export let columns: Record<string, string>;
	export let data: Record<string, Record<string, any>> = {};
	export let paginationSettings: PaginationSettings = {
		page: 0,
		limit: 5,
		total: 0,
		sizes: [5]
	};

	let sortColumn = '';
	let sortDirection = 1;

	// Derived rows array
	$: rowsArray = Object.entries(data).map(([id, row]) => ({ id, ...row }));
	$: paginationSettings.total = rowsArray.length;
	$: sortedRows = [...rowsArray];
	$: paginatedRows = sortedRows.slice(
			paginationSettings.page * paginationSettings.limit,
			(paginationSettings.page + 1) * paginationSettings.limit
	);

	function toggleSort(colKey: string) {
		if (sortColumn === colKey) {
			sortDirection = -sortDirection;
		} else {
			sortColumn = colKey;
			sortDirection = 1;
		}
		sortRows();
	}

	function sortRows() {
		if (!sortColumn) return;
		sortedRows = [...rowsArray].sort((a, b) => {
			const aVal = a[sortColumn];
			const bVal = b[sortColumn];
			if (aVal == null) return 1;
			if (bVal == null) return -1;
			if (typeof aVal === 'number' && typeof bVal === 'number') {
				return (aVal - bVal) * sortDirection;
			}
			return aVal.toString().localeCompare(bVal.toString()) * sortDirection;
		});
		paginationSettings.page = 0;
	}
</script>

<div class="section-wrapper p-8 space-y-4 w-full">
	<div class="flex items-center justify-between">
		{#if title}
			<h3 class="h3">{title}</h3>
		{/if}
		<button class="btn variant-filled-primary" on:click={() => dispatch('add')}>
			<Fa icon="plus" class="mr-2" />
			Add
		</button>
	</div>

	<div class="bordered-soft rounded-md overflow-hidden max-w-full">
		<div class="overflow-x-auto">
			<!-- Header -->
			<div
					class="grid w-full bg-surface-100-800-token gap-0.5"
					style="grid-template-columns: repeat({Object.keys(columns).length}, 1fr);">
				{#each Object.entries(columns) as [colKey, label]}
					<button
							class="p-4 break-words text-center cursor-pointer select-none"
							on:click={() => toggleSort(colKey)}>
						{label}
						{#if sortColumn === colKey}
							<span>{sortDirection === 1 ? ' ▲' : ' ▼'}</span>
						{/if}
					</button>
				{/each}
			</div>

			<!-- Body -->
			{#each paginatedRows as row}
				<div
						class="grid w-full border-t border-color hover:variant-soft-primary gap-0.5 p-1"
						style="grid-template-columns: repeat({Object.keys(columns).length}, 1fr);"
						role="button"
						tabindex="0"
						on:click={() => dispatch('edit', { id: row.id })}
						on:keydown={(e) => (e.key === 'Enter' || e.key === ' ') && dispatch('edit', { id: row.id })}>

					{#each Object.keys(columns) as colKey}
						<slot
								name="cell"
								row={row}
								colKey={colKey}
								value={row[colKey]}>
							<!-- Default: render value as text -->
							<p class="p-4 break-words text-center">{row[colKey] ?? ''}</p>
						</slot>
					{/each}
				</div>
			{/each}
		</div>
	</div>

	<div class="flex items-center justify-center space-x-4 mt-4">
		<Paginator
				settings={paginationSettings}
				on:change={(e) => (paginationSettings = e.detail.settings)}
		/>
	</div>
</div>
