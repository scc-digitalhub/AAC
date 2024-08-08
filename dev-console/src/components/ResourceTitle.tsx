import {
    useRecordContext,
    useResourceContext,
    useResourceDefinition,
} from 'react-admin';
import { PageTitle } from '../components/PageTitle';
import BlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import { isValidElement, ReactElement } from 'react';

export const ResourceTitle = (props: { icon?: ReactElement }) => {
    const { icon } = props;
    const record = useRecordContext();
    const resource = useResourceContext();
    const definition = useResourceDefinition();

    if (!record || !definition) {
        return null;
    }

    const ResourceIcon = definition.icon || BlankIcon;
    const iconEl =
        icon && isValidElement(icon) ? (
            icon
        ) : (
            <ResourceIcon
                fontSize="large"
                sx={{ fontSize: '96px' }}
                color="primary"
            />
        );

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
            icon={iconEl}
        />
    );
};
