<!-- RegistryDropdown.svelte -->
<script lang="ts">
    import Dropdown from './Dropdown.svelte';
    import { popup, type PopupSettings, ListBox, ListBoxItem } from '@skeletonlabs/skeleton';
    import Fa from 'svelte-fa';
    import { equals } from '$lib/duui/utils/text';
    import { faCheck, faChevronDown } from '@fortawesome/free-solid-svg-icons';
    import type { Placement } from '@floating-ui/dom'
    import { onMount, createEventDispatcher } from 'svelte';
    import type {DUUIComponentMetaData, DUUIRegistryEntry} from "$lib/duui/component";
    import Tip from "$lib/svelte/components/Tip.svelte"; // wherever you put the types


    const empty: DUUIRegistryEntry = {
        _id: "",
        name: "",
        meta_data: [{
            tag: "",
            search_tags: [],
            documentation: "",
            description: "",
            short_description: "",
            references: [],
            language: {
                name: "",
                code: ""
            },
            required_types: [],
            resulting_types: []
        }],
        registry_id: "",
        registry_name: "",
        registry_url: "",
    }

    let dropdownElement: HTMLButtonElement;
    let popupWidth = 'auto';

    /** Props */
    let selectedRegistryEndpoint: string = "";
    let filteredRegistryEndpoints: Map<string, string> = new Map<string, string>();


    let entries: DUUIRegistryEntry[] = [];
    let selectedEntry: DUUIRegistryEntry = empty;
    let selectedTag: string = "";
    let selectedMetaData: DUUIComponentMetaData = empty.meta_data[0];

    let filterTerm = '';
    const dispatch = createEventDispatcher();


    const reset = () => {
        selectedEntry = empty;
        selectedTag = "";
        selectedMetaData = empty.meta_data[0];
    }

    const change = () => {

        dispatch('change', { entry: selectedEntry, tag: selectedTag, metaData: selectedMetaData });
    }

    const entryDropdown: PopupSettings = {
        event: 'click',
        target: 'entry-dropdown',
        placement: 'bottom-start',
        closeQuery: '.listbox-item',
        middleware: { offset: 4 }
    };

    const filterRegistry = async (query: string) => {

        const response = await fetch(`/api/components/registry?endpoint=${selectedRegistryEndpoint}`, {
            method: 'PUT',
            body: JSON.stringify({
                registrySearchQuery: query
            })
        })

        if (response.ok) {
            entries = await response.json()



            if (!entries.find(e => e._id === selectedEntry?._id)) {
                reset()
                change()
            }
        }
    }

    const fetchFilteredRegistryEndpoints = async () => {
        const response = await fetch(`/api/users/registry?filter=True`, {
            method: 'GET'
        })



        if (response.ok) {
            const filreg: DUUIRegistries = await response.json();

            return new Map<string, string>(
                Object.entries(filreg).map(([key, value]) => [value.endpoint, value.name] as [string, string])
            );


            // alert(JSON.stringify(Object.fromEntries(filteredRegistryEndpoints)))
        }
        return new Map<string, string>()
    }

    const fetchRegistry = async () => {

        const response = await fetch(`/api/components/registry?endpoint=${selectedRegistryEndpoint}`, {
            method: 'GET'
        })

        if (response.ok) {
            entries = await response.json()
        }
    }

    onMount(async () => {

        filteredRegistryEndpoints = await fetchFilteredRegistryEndpoints();
        await fetchRegistry();

        if (dropdownElement) {
            popupWidth = `${dropdownElement.offsetWidth}px`;
        }
    })


    let debounceTimeout: ReturnType<typeof setTimeout>;

    function handleFilterInput(event: Event) {
        filterTerm = (event.target as HTMLInputElement).value;

        clearTimeout(debounceTimeout);
        // wait 300ms after the last keystroke
        debounceTimeout = setTimeout(() => {
            applyFilter();
        }, 300);
    }

    // when filter button clicked, re-fetch entries
    async function applyFilter() {
        await filterRegistry(filterTerm);
    }

    // whenever selection changes, reset tag and notify parent

    $: if (selectedEntry) {
        // default to first meta-data tag if none selected
        if (!selectedTag && selectedEntry.meta_data.length) {
            selectedMetaData = selectedEntry.meta_data[0];
            selectedTag = selectedEntry.meta_data[0].tag;
        }

        change()
    }
</script>

<div class="w-full flex gap-6">
    <div class="label flex-col w-[65%]">
        <span class="form-label text-start">DUUI-Registry Selection</span>

        <!-- Main dropdown for entries -->
        <button
                bind:this={dropdownElement}
                class=" w-full flex items-center !justify-between gap-2 px-3 py-2 leading-6 border border-color rounded-md input-wrapper"
                use:popup={entryDropdown}
                data-popup-target="entry-dropdown"
        >
            <span>{selectedEntry.name || 'Select Registry Entry'}</span>
            <Fa icon={faChevronDown} />
        </button>
    </div>

    <div class="label flex-col w-[15%] ">
        {#if selectedEntry}
            <!-- Side dropdown for tags -->
            <Dropdown
                    label="Meta‐Tag"
                    bind:value={selectedTag}
                    options={selectedEntry.meta_data.map(m => m.tag)}
                    on:change={() => {
                    selectedMetaData = selectedEntry.meta_data.find(m => m.tag === selectedTag) ?? empty.meta_data[0];
                    change();
                }}
            />
        {/if}
    </div>

    <!-- Popup pane -->
    <div data-popup="entry-dropdown" class="fixed overflow-y-auto h64 mt-1 z-10" style="width: {popupWidth};">
        <div class="popup-solid p-2 md:min-w-[220px] overflow-scroll max-h-96">
            <!-- Filter inside dropdown pane -->
            <div class="flex items-center  mb-2 gap-2">
                {#key filteredRegistryEndpoints.size}
                    <Dropdown
                            name="availableRegistries"
                            label="Registries"
                            bind:value={selectedRegistryEndpoint}
                            options={filteredRegistryEndpoints}

                            on:change={async () => {
                                await fetchRegistry();
                                reset();
                                change();
                            }}
                    />
                {/key}

                {#if selectedRegistryEndpoint }
                    <input
                            type="text"
                            placeholder="Filter..."
                            bind:value={filterTerm}
                            on:input={handleFilterInput}
                            class="px-2 py-1 border rounded-md"
                    />
                    <button on:click={applyFilter} class="px-3 py-1 variant-filled-primary text-white rounded-md">
                        Filter
                    </button>
                {/if}

    <!--                <button on:click={fetchRegistry} class="px-3 py-1 bg-blue-600 text-white rounded-md">-->
    <!--                    Reset-->
    <!--                </button>-->
            </div>

            <div class="divider my-4 border-t border-gray-300"></div>

            {#if filteredRegistryEndpoints.size < 1}
                <Tip tipTheme="error">
                        You have no registries available. Access can be granted by an administrator.
                </Tip>
            {:else if !selectedRegistryEndpoint }
                <Tip tipTheme="tertiary">
                    Please select a registry endpoint to view entries.
                </Tip>
            {:else}
                <ListBox class="overflow-hidden" rounded="rounded-md" spacing="space-y-2">
                    {#each entries as entry (entry._id)}
                        <ListBoxItem
                                class="listbox-item"
                                on:change
                                bind:group={selectedEntry}
                                name="entry-dropdown"
                                value={entry}
                                rounded="rounded-md"
                                hover="hover:bg-surface-100-800-token"
                                active="variant-filled-primary"
                        >
                            <svelte:fragment slot="lead">
                                <Fa class={equals(selectedEntry?._id, entry._id) ? '' : 'invisible'} icon={faCheck} />
                            </svelte:fragment>
                            <div class="flex justify-between">
                                <span>{entry.name}</span>
                                <small class="text-gray-500">
                                    {entry.meta_data[0]?.language?.name ?? '—'}
                                </small>
                            </div>
                        </ListBoxItem>
                    {/each}
                </ListBox>
            {/if}

        </div>
    </div>
</div>




