<script lang="ts">
	import { Fa } from 'svelte-fa';
	import { createEventDispatcher, onMount } from 'svelte'
	import Paginator from './Paginator.svelte'
	import type { IconDefinition } from '@fortawesome/free-solid-svg-icons'
	import Chip from './Chip.svelte'


	const dispatcher = createEventDispatcher()

	type ColumnMapping = {
		[key: string]: {
			icon?: IconDefinition;
			mapper: (any) => string;
		}
	}

	export let data: {[id: string]: NonNullable<unknown>} = {};
	export let columns;
	export let title;
	export let columnMapping: ColumnMapping = {};
	export let paginationSettings: PaginationSettings = {
		page: 0,
		limit: 5,
		total: 100,
		sizes: [5]
	}

	// Sorting, and pagination state:

	let sortColumn = "";
	let sortDirection = 1; // 1 for ascending, -1 for descending

	onMount(() => {
		paginationSettings.limit = 10;
		paginationSettings.limit = 5;
	});

	// Convert the data object to an array of rows with an 'id' property.
	$: rowsArray = Object.entries(data).map(([id, row]) => ({ id, ...row }));

	$: paginationSettings.total = rowsArray.length;

	// Sorting: Sort filtered rows based on sortColumn and sortDirection
	$: sortedRows = [...rowsArray];

	// And paginatedRows is derived from sortedRows.
    $: paginatedRows = sortedRows.slice(
		paginationSettings.page * paginationSettings.limit,
		(paginationSettings.page + 1) * paginationSettings.limit
	);

	// Toggle sorting on a column. Resets pagination to page 1.
	function sortRows() {
        if (!sortColumn) {
            sortedRows = rowsArray;
        } else {
            sortedRows = [...rowsArray].sort((a, b) => {
                let aValue = a[sortColumn];
                let bValue = b[sortColumn];
                if (aValue == null) return 1;
                if (bValue == null) return -1;
                if (typeof aValue === "number" && typeof bValue === "number") {
                    return (aValue - bValue) * sortDirection;
                }
                return aValue.toString().localeCompare(bValue.toString()) * sortDirection;
            });
        }
        // Reset pagination 
        paginationSettings.page = 0;
    }

	function toggleSort(col) {
		if (sortColumn === col) {
			sortDirection = -sortDirection;
		} else {
			sortColumn = col;
			sortDirection = 1;
		}

		sortRows();
	}

	function start() {
		// Reset pagination
		// rowsArray = Object.entries(data).map(([id, row]) => ({ id, ...row }));
		// sortedRows = [...rowsArray];
	}	

	start();
</script>

<div class="section-wrapper p-8 space-y-4 w-full">
	<div class="flex items-center justify-between">
		{#if title}
			<h3 class="h3">{title}</h3>
		{/if}

		<!-- Filter Input -->
		<div class="mb-4">
			<!-- Add Button -->
			<button class="btn variant-filled-primary" on:click={() => dispatcher('add')}>
				<Fa icon="plus" class="mr-2"/>
				Add
			</button>
		</div>
	</div>
	

	<div class="bordered-soft rounded-md overflow-hidden max-w-full">
		<div class="overflow-x-auto">
			<!-- Table Header -->
			<div
				class="grid w-full bg-surface-100-800-token gap-0.5"
				style={`grid-template-columns: repeat(${Object.keys(columns).length}, 1fr);`}>
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

			<!-- Table Body -->
			{#each paginatedRows as row}
				<div
						class="grid w-full border-t border-color max-h-12 overflow-hidden hover:variant-soft-primary gap-0.5 p-1"
						style={`grid-template-columns: repeat(${Object.keys(columns).length}, 1fr);`}
						role="button"
						tabindex=0
						on:click={() => dispatcher('edit', { id: row.id })}
						on:keydown={(e) => {
							if (e.key === 'Enter' || e.key === ' ') {
								dispatcher('edit', { id: row.id });
							}
						}}
						>
					{#each Object.keys(columns) as colKey}
						{#if columnMapping[colKey]}
							<div class="chip-container flex flex-wrap items-start gap-0.5 p-1">
								{#each row[colKey] as colItem}
									<Chip text={columnMapping[colKey].mapper(colItem)} leftIcon={columnMapping[colKey].icon} />
								{/each}
							</div>
						{:else}
							<p class="p-4 break-words text-center">
								{row[colKey] !== undefined ? row[colKey] : ''}
							</p>
						{/if}
					{/each}
				</div>
			{/each}
		</div>
	</div>

	<!-- Pagination Controls -->
	<div class="flex items-center justify-center space-x-4 mt-4">
		<Paginator 
			settings={paginationSettings}  
			on:change={(e) => {
				paginationSettings = e.detail.settings;
			}}
		/>
	</div>
</div>
