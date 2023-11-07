import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    DateField,
    TopToolbar,
    DateInput,
    SelectInput,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import ContentFilter from '@mui/icons-material/FilterList';
import { AuditDetails } from './AuditDetails';
import { Typography } from '@mui/material';

import { useForm, FormProvider } from 'react-hook-form';
import { Box, Button } from '@mui/material';

export const AuditList = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    useListContext<any>();
    return (
        <>
            <br />
            <Typography variant="h5" sx={{ mt: 1 }}>
                Audit events
            </Typography>
            <Typography variant="h6">Review and inspect events</Typography>
            <List
                actions={<ListActions />}
                queryOptions={options}
                // filters={RealmFilters}
            >
                <Datagrid
                    rowClick="expand"
                    expand={<AuditDetails />}
                    bulkActionButtons={false}
                >
                    <DateField source="time" showTime />
                    <TextField source="type" />
                    <TextField source="principal" />
                </Datagrid>
            </List>
        </>
    );
};

const RealmFilters = [<SearchInput placeholder="Type" source="q" alwaysOn />];

const PostFilterButton = () => {
    const { showFilter } = useListContext();
    return (
        <Button
            size="small"
            color="primary"
            onClick={() => showFilter('main', null)}
            startIcon={<ContentFilter />}
        >
            Filter
        </Button>
    );
};

const PostFilterForm = () => {
    const { displayedFilters, filterValues, setFilters, hideFilter } =
        useListContext();

    const form = useForm({
        defaultValues: filterValues,
    });

    if (!displayedFilters.main) return null;

    const onSubmit = (values: any) => {
        if (Object.keys(values).length > 0) {
            if (values.before) {
                let before = new Date(values.before);
                values.before = before.toISOString();
            }
            if (values.after) {
                let after = new Date(values.after);
                values.after = after.toISOString();
            }
            if (values.type == 'All') {
                values.type = null;
            }
            setFilters(values, null);
        } else {
            hideFilter('main');
        }
    };

    const dateFormatter = (data: any) => {
        console.log(typeof data);
        if (isNaN(data)) {
            data = new Date(data);
        }
        return data;
    };

    const resetFilter = () => {
        setFilters({}, []);
    };

    return (
        <FormProvider {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)}>
                <Box display="inherit" mb={1}>
                    <Box component="span" mr={2}>
                        <DateInput
                            helperText={false}
                            // validate={[required()]}
                            source="before"
                        />
                    </Box>
                    <Box component="span" mr={2}>
                        <DateInput helperText={false} source="after" />
                    </Box>
                    <Box mr={2}>
                        <SelectInput
                            defaultValue={'All'}
                            source="type"
                            label="Type"
                            choices={[
                                { id: 'All', name: 'All' },
                                {
                                    id: 'USER_AUTHENTICATION_SUCCESS',
                                    name: 'USER_AUTHENTICATION_SUCCESS',
                                },
                                {
                                    id: 'USER_AUTHENTICATION_FAILURE',
                                    name: 'USER_AUTHENTICATION_FAILURE',
                                },
                                {
                                    id: 'CLIENT_AUTHENTICATION_SUCCESS',
                                    name: 'CLIENT_AUTHENTICATION_SUCCESS',
                                },
                                {
                                    id: 'CLIENT_AUTHENTICATION_FAILURE',
                                    name: 'CLIENT_AUTHENTICATION_FAILURE',
                                },
                                { id: 'TOKEN_GRANT', name: 'TOKEN_GRANT' },
                            ]}
                        />
                    </Box>
                    <Box component="span" mr={2}>
                        <Button
                            variant="outlined"
                            color="primary"
                            type="submit"
                        >
                            Filter
                        </Button>
                    </Box>
                    <Box component="span">
                        <Button variant="outlined" onClick={resetFilter}>
                            Close
                        </Button>
                    </Box>
                </Box>
            </form>
        </FormProvider>
    );
};

const ListActions = () => (
    <Box width="100%">
        <TopToolbar>
            <PostFilterButton />
        </TopToolbar>
        <PostFilterForm />
    </Box>
);