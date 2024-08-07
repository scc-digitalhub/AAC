import {
    useRecordContext,
    useResourceContext,
    useResourceDefinition,
} from 'react-admin';
import { PageTitle } from '../components/PageTitle';
import BlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';

export const ResourceTitle = () => {
    const record = useRecordContext();
    const resource = useResourceContext();
    const definition = useResourceDefinition();

    if (!record || !definition) {
        return null;
    }

    const ResourceIcon = definition.icon || BlankIcon;

    const recordRepresentation =
        typeof definition.recordRepresentation === 'function'
            ? definition.recordRepresentation(record)
            : typeof definition.recordRepresentation === 'string'
            ? definition.recordRepresentation
            : undefined;

    const displayText = record.name || recordRepresentation || record.id;

    return (
        <PageTitle
            text={displayText}
            secondaryText={record.id}
            copy={true}
            icon={
                <ResourceIcon
                    fontSize="large"
                    sx={{ fontSize: '96px' }}
                    color="primary"
                />
            }
        />
    );
};
