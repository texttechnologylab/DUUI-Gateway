<!-- RegistryDropdown.svelte -->
<script lang="ts">
    import Dropdown from './Dropdown.svelte';
    import {popup, type PopupSettings, ListBox, ListBoxItem, getToastStore} from '@skeletonlabs/skeleton';
    import Fa from 'svelte-fa';
    import { equals } from '$lib/duui/utils/text';
    import {
        faCheck,
        faChevronDown, faClose,
        faFilter,
        faFilterCircleDollar, faFilterCircleXmark,
        faInfo
    } from '@fortawesome/free-solid-svg-icons';
    import type { Placement } from '@floating-ui/dom'
    import { onMount, createEventDispatcher } from 'svelte';
    import type {DUUIComponentMetaData, DUUIRegistryEntry} from "$lib/duui/component";
    import Tip from "$lib/svelte/components/Tip.svelte";
    import {errorToast, infoToast} from "$lib/duui/utils/ui";
    import Popup from "$lib/svelte/components/Popup.svelte";
    import Link from "$lib/svelte/components/Link.svelte";
    import Chip from "$lib/svelte/components/Chip.svelte"; // wherever you put the types


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
            language: [{
                name: "",
                code: ""
            }],
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


    let displayFilterFlags = false;
    let filterFlagStates = {
        Name: "false",
        ResultingTypes: "false",
        RequiredTypes: "false",
        SearchTags: "false",
        Language: "false",
        ShortDescription: "false"
    }

    const dispatch = createEventDispatcher();

    const toastStore = getToastStore()


    const reset = () => {
        selectedEntry = empty;
        selectedTag = "";
        selectedMetaData = empty.meta_data[0];
    }

    const change = () => {

        dispatch('change', { entry: selectedEntry, tag: selectedTag, metaData: selectedMetaData });
    }

    const filterRegistry = async (query: string) => {

        if (!query) {
            await fetchRegistry();
            return;
        }

        const response = await fetch(`/api/components/registry?endpoint=${selectedRegistryEndpoint}`, {
            method: 'PUT',
            body: JSON.stringify({
                registrySearchQuery: query,
                flagName: filterFlagStates.Name,
                flagResultingTypes: filterFlagStates.ResultingTypes,
                flagRequiredTypes: filterFlagStates.RequiredTypes,
                flagSearchTags: filterFlagStates.SearchTags,
                flagLanguage: filterFlagStates.Language,
                flagShortDescription: filterFlagStates.ShortDescription


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

    const fetchUserRegistryEndpoints = async () => {
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

        if (!selectedRegistryEndpoint) {
            // toastStore.trigger(infoToast("No registry selected."))
            entries = [];
            return;
        }

        const response = await fetch(`/api/components/registry?endpoint=${selectedRegistryEndpoint}`, {
            method: 'GET'
        })

        if (response.ok) {
            entries = await response.json()
        } else {
            toastStore.trigger(errorToast(`ERROR ${response.status}: ${response.statusText} \n Failed to fetch registry entries. Registry is not reachable.`))
            entries = [];
        }
    }

    onMount(async () => {

        filteredRegistryEndpoints = await fetchUserRegistryEndpoints();
        await fetchRegistry();

        // if (dropdownElement) {
        //     popupWidth = `${dropdownElement.offsetWidth}px`;
        // }
    })

    function handleWheel(event: WheelEvent) {
        event.preventDefault();
        (event.currentTarget as HTMLElement).scrollLeft += event.deltaY;
    }

    let debounceTimeout: ReturnType<typeof setTimeout>;

    function handleFilterInput(event: Event) {
        filterTerm = (event.target as HTMLInputElement).value;
        filterTerm = filterTerm.trim();

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

    let displayEntries: boolean = false;

    $: if (selectedRegistryEndpoint) {
        (async () => {
            await fetchRegistry();
            reset();
            change();
        })();

    }

    $: displayEntries = displayEntries

    $: displayFilterFlags = displayFilterFlags

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
                on:click={() => {
                    displayEntries = !displayEntries;
                }}

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

</div>
<!-- Popup pane style="width: {popupWidth};" overflow-y-auto -->
{#if displayEntries}
    <div class="w-full" >
        <div class="border-2 p-2 rounded-md">
            <!-- Filter inside dropdown pane -->
            <div class="flex justify-between  mb-2 gap-2">
                {#key filteredRegistryEndpoints.size}
                    <Dropdown
                            name="availableRegistries"
                            label="Registries"
                            bind:value={selectedRegistryEndpoint}
                            options={filteredRegistryEndpoints}
                            initFirst={true}
                            on:change={async () => {
                                await fetchRegistry();
                                reset();
                                change();
                            }}
                    />
                {/key}

                {#if selectedRegistryEndpoint }
                        <div class="w-8/12 mt-7">
                            <div class="relative input-wrapper">
                                <input
                                        type="text"
                                        placeholder="Filter..."
                                        bind:value={filterTerm}
                                        on:input={handleFilterInput}
                                        class=" w-max border-none bg-transparent focus:ring-0  appearance-none "
                                />

                                <button class="absolute top-1/4 right-0 transform -translate-y-3/4 flex items-center mt-2 pr-3">
                                    <Fa icon={faFilter} size="lg" class="text-primary-600" />
                                </button>

                                <hr >
                                <div class="flex flex-row mt-2 w-full overflow-x-scroll" on:wheel={handleWheel}>
                                    {#each Object.entries(filterFlagStates) as [flagKey, flagValue]}
                                        {#if flagValue === "true"}
                                            <small class="max-w-20 h-15 px-1 py-0.5 mr-1">
                                                <Chip text={flagKey}>
                                                        <span slot="icon-right">
                                                            <button class="hover:text-error-500 duration-300 transition-colors"
                                                                    on:click={() => {
                                                                        filterFlagStates[flagKey] = "false";
                                                                        applyFilter();
                                                                    }}>

                                                                <Fa icon={faClose}  />

                                                            </button>
                                                        </span>
                                                </Chip>
                                            </small>
                                        {/if}
                                    {/each}
                                </div>
                            </div>

                            <div class="w-full mt-3 relative mb-4">
                                <div class="flex-row flex w-full overflow-x-scroll" on:wheel={handleWheel}>
                                    {#each Object.entries(filterFlagStates) as [flagKey, flagValue]}
                                        {#if flagValue === "false"}
                                            <small class="max-w-20 h-15 px-1 py-0.5 mr-1">
                                                <Chip text={flagKey} color="tertiary">
                                                    <span slot="icon-right">
                                                        <button class="hover:text-green-400 duration-300 transition-colors"
                                                                on:click={ () => {
                                                                    filterFlagStates[flagKey] = "true";
                                                                    applyFilter();
                                                                }}>

                                                            <Fa icon={faCheck}  />

                                                        </button>
                                                    </span>
                                                </Chip>
                                            </small>
                                        {/if}
                                    {/each}
                                </div>
                            </div>
                        </div>




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
            {:else if !entries.length }
                <Tip tipTheme="tertiary">
                   No entries found in the selected registry.
                </Tip>
            {:else}
                <div class="overflow-visible">


                    <div class="flex justify-between p-2 border-b border-gray-300 font-bold">
                        <span>Entry Name</span>
                        <span>Language</span>
                        <span>Output Types</span>
                    </div>
                    <ListBox class="overflow-scroll h-60" rounded="rounded-md" spacing="space-y-2">

                        {#each entries as entry (entry._id)}
                            <ListBoxItem
                                    class="listbox-item"
                                    on:change
                                    bind:group={selectedEntry}
                                    name="entry-dropdown"
                                    value={entry}
                                    rounded="rounded-md"
                                    hover="hover:bg-surface-100-800-token"
                                    active="variant-soft-primary"

                            >
<!--                                <svelte:fragment slot="lead">-->
<!--                                    <Fa class={equals(selectedEntry?._id, entry._id) ? '' : 'invisible'} icon={faCheck} />-->
<!--                                </svelte:fragment>-->
                                <div class="flex justify-between ">
                                    <span>

                                        {#if !entry.meta_data[0]?.documentation}
                                            {entry.name}
                                        {:else}

                                            <Popup autoPopupWidth={true} arrow={false} position="bottom">
                                                <svelte:fragment slot="trigger">
                                                    <span class="items-start gap-1">
                                                        {entry.name}
<!--                                                        <Fa icon={faInfo} size="xs" class=" variant-soft-primary" />-->
                                                        <div class="inline-flex items-center justify-center rounded-full border variant-soft-primary p-2">
                                                            <Fa icon={faInfo} size="xs" class="text-variant-soft-primary" />
                                                        </div>
                                                    </span>
                                                </svelte:fragment>
                                                <svelte:fragment slot="popup" >

                                                    <div class="bg-primary-50-900-token bg-transparent absolute p-3 rounded-md w-96 overflow-y-auto h-36">
                                                        <small class="max-w-prose text-primary-700">
                                                            {entry.meta_data[0].description}
                                                        </small>

                                                        <span class="mb-2">
                                                            <Link href={entry.meta_data[0].documentation} underline={true}>
                                                                <small>Documentation</small>
                                                            </Link>

                                                        </span>

                                                    </div>
                                                </svelte:fragment>
                                            </Popup>
                                        {/if}

                                    </span>
                                    <small class="text-gray-500">
                                        {entry.meta_data[0]?.language?.map(lo => lo.name).toString() ?? '—'}
                                    </small>
                                    <small class="text-gray-500">
                                        {entry.meta_data[0]?.resulting_types?.map(rt => rt.toString().split(".").pop()).toString() ?? '—'}
                                    </small>
                                </div>
                            </ListBoxItem>
                        {/each}
                    </ListBox>
                </div>
            {/if}

        </div>
    </div>
{/if}




